package org.sfa.volunteer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.sfa.volunteer.config.AvailabilityConfiguration;
import org.sfa.volunteer.aspect.LoggingAspect;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySlot;
import org.sfa.volunteer.dto.request.AvailabilitySlotRequest;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.response.OnboardingAvailabilitySlotResponse;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.repository.AvailabilityAccountRepository;
import org.sfa.volunteer.repository.LegacyAvailabilityRepository;
import org.sfa.volunteer.repository.OnboardingAvailabilityRepository;
import org.sfa.volunteer.repository.OnboardingAvailabilitySchema;
import org.sfa.volunteer.support.PostgresTestDatabase;
import org.sfa.volunteer.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OnboardingAvailabilityService.class, AvailabilityScheduleService.class,
        UserServiceImpl.class,
        AvailabilityAccountRepository.class, LegacyAvailabilityRepository.class, OnboardingAvailabilityRepository.class,
        OnboardingAvailabilitySchema.class, AvailabilityConfiguration.class,
        LoggingAspect.class, OnboardingAvailabilityPostgresTest.LoggingConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfSystemProperty(named = "ba33.pg.enabled", matches = "true")
class OnboardingAvailabilityPostgresTest {
    @TestConfiguration
    @EnableAspectJAutoProxy
    static class LoggingConfiguration {
        @Bean @Primary TestClock fixedOnboardingClock() {
            return new TestClock();
        }
    }

    static class TestClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-07-06T12:00:00Z"));
        void set(Instant instant) { now.set(instant); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return now.get(); }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        PostgresTestDatabase.configure(properties, "ba33_t2");
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET TIME ZONE 'America/Los_Angeles'");
    }

    @SpyBean JdbcTemplate jdbc;
    @Autowired OnboardingAvailabilityService onboarding;
    @Autowired UserServiceImpl profiles;
    @Autowired TestClock clock;

    @BeforeEach
    void fixture() {
        clock.set(Instant.parse("2026-07-06T12:00:00Z"));
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS ba33_t2");
        jdbc.execute("DROP TABLE IF EXISTS user_availability");
        jdbc.execute("DROP TABLE IF EXISTS volunteer_details");
        jdbc.execute("DROP TABLE IF EXISTS users");
        jdbc.execute("""
                CREATE TABLE users (
                    user_id varchar(64) PRIMARY KEY,
                    time_zone varchar(100),
                    primary_email_address varchar(255),
                    promotion_wizard_stage integer,
                    promotion_wizard_last_updated_at timestamp without time zone,
                    is_emergency_available boolean DEFAULT false NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE volunteer_details (user_id varchar(64) PRIMARY KEY REFERENCES users(user_id),
                    availability_days jsonb, availability_times jsonb)
                """);
        jdbc.execute("""
                CREATE TABLE user_availability (
                    user_availability_id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    user_id varchar(64) NOT NULL REFERENCES users(user_id),
                    day_of_week varchar(10) NOT NULL,
                    start_time timestamp without time zone NOT NULL,
                    end_time timestamp without time zone NOT NULL,
                    last_updated_at timestamp without time zone
                )
                """);
        jdbc.update("INSERT INTO users(user_id,time_zone,primary_email_address,promotion_wizard_stage) VALUES (?,?,?,?)",
                "SID-T2", "Asia/Kolkata", "onboarding@example.test", 3);
        jdbc.update("INSERT INTO volunteer_details(user_id) VALUES (?)", "SID-T2");
    }

    @Test
    void savesLocalScheduleEmergencyPreferenceAndStepFourProgressTogether() {
        var saved = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
        var loaded = onboarding.read("SID-T2");
        assertThat(loaded).isEqualTo(saved);
        assertThat(loaded.userId()).isEqualTo("SID-T2");
        assertThat(loaded.isEmergencyAvailable()).isTrue();
        assertThat(loaded.promotionWizardStage()).isEqualTo(4);
        assertThat(loaded.availability()).extracting(OnboardingAvailabilitySlotResponse::dayOfWeek,
                OnboardingAvailabilitySlotResponse::startTime, OnboardingAvailabilitySlotResponse::endTime)
                .containsExactly(tuple("Monday", "09:00", "12:00"));
        assertThat(loaded.availability().get(0).id()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_last_updated_at FROM users WHERE user_id='SID-T2'",
                LocalDateTime.class)).isEqualTo(LocalDateTime.parse("2026-07-06T12:00:00"));
    }

    @Test
    void missingEmergencyMigrationReturnsFeatureUnavailableBeforeChangingScheduleOrProgress() {
        jdbc.execute("ALTER TABLE users DROP COLUMN is_emergency_available");
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
                    assertThat(failure.getHttpStatus()).isEqualTo(503);
                    assertThat(failure.getErrorCode()).isEqualTo("FEATURE_UNAVAILABLE");
                    assertThat(failure.getMessage()).doesNotContain("SELECT", "is_emergency_available", "jdbc:");
                });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_stage FROM users WHERE user_id='SID-T2'", Integer.class))
                .isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingAccountOrVolunteerReturnsANotFoundFailure(boolean removeAccount) {
        jdbc.update("DELETE FROM volunteer_details WHERE user_id='SID-T2'");
        if (removeAccount) {
            jdbc.update("DELETE FROM users WHERE user_id='SID-T2'");
        }
        String code = removeAccount ? "USER_NOT_FOUND" : "VOLUNTEER_NOT_FOUND";
        assertThatThrownBy(() -> onboarding.read("SID-T2"))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
                    assertThat(failure.getHttpStatus()).isEqualTo(404);
                    assertThat(failure.getErrorCode()).isEqualTo(code);
                });
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), false)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(404));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Not/AZone", "+05:30", "UTC+5"})
    void anUnusableStoredTimezoneRequiresAccountRemediationWithoutWrites(String timezone) {
        jdbc.update("UPDATE users SET time_zone = ? WHERE user_id='SID-T2'", timezone);
        assertThatThrownBy(() -> onboarding.read("SID-T2"))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
                    assertThat(failure.getHttpStatus()).isEqualTo(409);
                    assertThat(failure.getErrorCode()).isEqualTo("ACCOUNT_TIMEZONE_REQUIRED");
                });
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(409));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_stage FROM users WHERE user_id='SID-T2'", Integer.class))
                .isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ALTER TABLE user_availability DROP COLUMN start_time",
            "ALTER TABLE user_availability ALTER COLUMN end_time TYPE timestamp with time zone",
            "ALTER TABLE user_availability ALTER COLUMN last_updated_at TYPE time USING last_updated_at::time",
            "ALTER TABLE users RENAME COLUMN promotion_wizard_last_updated_at TO promotion_wizard_last_update_date",
            "ALTER TABLE users ALTER COLUMN is_emergency_available DROP NOT NULL",
            "DROP TABLE volunteer_details"
    })
    void anIncompatibleAvailabilitySchemaFailsBeforeAnyStateWrite(String fixtureChange) {
        jdbc.execute(fixtureChange);
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
                    assertThat(failure.getHttpStatus()).isEqualTo(503);
                    assertThat(failure.getErrorCode()).isEqualTo("FEATURE_UNAVAILABLE");
                });
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_stage FROM users WHERE user_id='SID-T2'", Integer.class))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
    }

    @Test
    void genericServiceLoggingDoesNotExposeTheAvailabilityRequestOrSchedule() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.start();
        logger.addAppender(captured);
        try {
            onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                    List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
            onboarding.read("SID-T2");
            profiles.updateAvailability(new UpdateAvailabilityRequest("SID-T2", "Asia/Kolkata",
                    List.of(new AvailabilitySlotRequest("Tuesday", "09:00", "12:00")), null, null, null));
            profiles.getAvailability("SID-T2");
            assertThat(captured.list).extracting(ILoggingEvent::getFormattedMessage)
                    .allSatisfy(message -> assertThat(message)
                            .doesNotContain("SID-T2", "09:00", "12:00", "isEmergencyAvailable"));
        } finally {
            logger.detachAppender(captured);
            captured.stop();
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {5})
    void replacementPersistsEmergencyFalseRemovesOldSlotsAndNeverRegressesProgress(Integer previousStage) {
        jdbc.update("UPDATE users SET promotion_wizard_stage = ? WHERE user_id='SID-T2'", previousStage);
        onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
        var replaced = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Tuesday", "14:00", "17:00")), false));
        assertThat(onboarding.read("SID-T2")).isEqualTo(replaced);
        assertThat(replaced.isEmergencyAvailable()).isFalse();
        assertThat(replaced.promotionWizardStage()).isEqualTo(previousStage == null ? 4 : 5);
        assertThat(replaced.availability()).extracting(OnboardingAvailabilitySlotResponse::dayOfWeek,
                OnboardingAvailabilitySlotResponse::startTime, OnboardingAvailabilitySlotResponse::endTime)
                .containsExactly(tuple("Tuesday", "14:00", "17:00"));
    }

    @Test
    void anExistingVolunteerWithoutASavedScheduleCanReadItsEmptyState() {
        var empty = onboarding.read("SID-T2");
        assertThat(empty.availability()).isEmpty();
        assertThat(empty.isEmergencyAvailable()).isFalse();
        assertThat(empty.promotionWizardStage()).isEqualTo(3);
    }

    @Test
    void aTimezoneAwareProgressColumnStillRecordsTheActualUtcInstant() {
        jdbc.execute("ALTER TABLE users ALTER COLUMN promotion_wizard_last_updated_at TYPE timestamp with time zone");
        onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
        var timestamp = jdbc.queryForObject("SELECT promotion_wizard_last_updated_at FROM users WHERE user_id='SID-T2'",
                OffsetDateTime.class);
        assertThat(timestamp.toInstant()).isEqualTo(Instant.parse("2026-07-06T12:00:00Z"));
    }

    @Test
    void anOverlappingEditPreservesTheCompletePreviouslySavedState() {
        var before = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00"),
                        new OnboardingAvailabilitySlot("Monday", "10:00", "11:00")), false)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
                    assertThat(failure.getHttpStatus()).isEqualTo(400);
                    assertThat(failure.getErrorCode()).isEqualTo("BAD_REQUEST");
                });
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
    }

    static Stream<OnboardingAvailabilitySaveRequest> invalidEnvelopes() {
        var valid = List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00"));
        return Stream.of((OnboardingAvailabilitySaveRequest) null,
                new OnboardingAvailabilitySaveRequest(null, 4, valid, false),
                new OnboardingAvailabilitySaveRequest(" ", 4, valid, false),
                new OnboardingAvailabilitySaveRequest("SID-T2", null, valid, false),
                new OnboardingAvailabilitySaveRequest("SID-T2", 3, valid, false),
                new OnboardingAvailabilitySaveRequest("SID-T2", 4, valid, null),
                new OnboardingAvailabilitySaveRequest("SID-T2", 4, null, true),
                new OnboardingAvailabilitySaveRequest("SID-T2", 4, List.of(), true),
                new OnboardingAvailabilitySaveRequest("SID-T2", 4, Collections.singletonList(null), false));
    }

    @ParameterizedTest
    @MethodSource("invalidEnvelopes")
    void incompleteOrWrongStepRequestsCannotChangeSavedOnboardingState(OnboardingAvailabilitySaveRequest request) {
        var before = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
        assertThatThrownBy(() -> onboarding.save(request))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(400));
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
    }

    static Stream<OnboardingAvailabilitySlot> invalidLocalIntervals() {
        return Stream.of(
                new OnboardingAvailabilitySlot(null, "09:00", "12:00"),
                new OnboardingAvailabilitySlot("", "09:00", "12:00"),
                new OnboardingAvailabilitySlot(" Monday", "09:00", "12:00"),
                new OnboardingAvailabilitySlot("Weekdays", "09:00", "12:00"),
                new OnboardingAvailabilitySlot("Weekends", "09:00", "12:00"),
                new OnboardingAvailabilitySlot("Holiday", "09:00", "12:00"),
                new OnboardingAvailabilitySlot("Monday", null, "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:00", null),
                new OnboardingAvailabilitySlot("Monday", "", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:00", ""),
                new OnboardingAvailabilitySlot("Monday", "9:00", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:0", "12:00"),
                new OnboardingAvailabilitySlot("Monday", " 09:00", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:00", "12:00 "),
                new OnboardingAvailabilitySlot("Monday", "09:00:00", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:00", "12:00:00"),
                new OnboardingAvailabilitySlot("Monday", "2026-07-06T09:00:00", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:00Z", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:00+05:30", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "24:00", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:00", "24:00"),
                new OnboardingAvailabilitySlot("Monday", "09:60", "12:00"),
                new OnboardingAvailabilitySlot("Monday", "09:00", "09:00"),
                new OnboardingAvailabilitySlot("Monday", "22:00", "02:00"));
    }

    @ParameterizedTest
    @MethodSource("invalidLocalIntervals")
    void onlyCompleteSameDayMinutePrecisionIntervalsCanReplaceASchedule(OnboardingAvailabilitySlot interval) {
        var before = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), false));
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(interval), true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(400));
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
    }

    @Test
    void everydayExpandsToSevenIndependentlyReadableWeekdays() {
        var saved = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Everyday", "09:00", "12:00")), true));
        assertThat(onboarding.read("SID-T2")).isEqualTo(saved);
        assertThat(saved.availability()).extracting(OnboardingAvailabilitySlotResponse::dayOfWeek)
                .containsExactly("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        assertThat(saved.availability()).allSatisfy(slot -> {
            assertThat(slot.startTime()).isEqualTo("09:00");
            assertThat(slot.endTime()).isEqualTo("12:00");
        });
    }

    static Stream<List<OnboardingAvailabilitySlot>> duplicateOrGroupedOverlaps() {
        return Stream.of(
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00"),
                        new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")),
                List.of(new OnboardingAvailabilitySlot("Everyday", "09:00", "12:00"),
                        new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")),
                List.of(new OnboardingAvailabilitySlot("Everyday", "09:00", "12:00"),
                        new OnboardingAvailabilitySlot("Sunday", "10:00", "11:00")));
    }

    @ParameterizedTest
    @MethodSource("duplicateOrGroupedOverlaps")
    void duplicateAndEverydayIntersectionsAreRejectedAfterExpansion(List<OnboardingAvailabilitySlot> intervals) {
        var before = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "14:00", "16:00")), false));
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4, intervals, true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
                    assertThat(failure.getHttpStatus()).isEqualTo(400);
                    assertThat(failure.getMessage()).contains("overlap");
                });
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
    }

    @Test
    void adjacentSlotsAndIdenticalTimesOnDifferentDaysAreIndependentAndSorted() {
        var saved = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4, List.of(
                new OnboardingAvailabilitySlot("Monday", "12:00", "14:00"),
                new OnboardingAvailabilitySlot("Tuesday", "09:00", "12:00"),
                new OnboardingAvailabilitySlot("Sunday", "00:00", "23:59"),
                new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), false));
        assertThat(onboarding.read("SID-T2")).isEqualTo(saved);
        assertThat(saved.availability()).extracting(OnboardingAvailabilitySlotResponse::dayOfWeek,
                OnboardingAvailabilitySlotResponse::startTime, OnboardingAvailabilitySlotResponse::endTime)
                .containsExactly(tuple("Monday", "09:00", "12:00"), tuple("Monday", "12:00", "14:00"),
                        tuple("Tuesday", "09:00", "12:00"), tuple("Sunday", "00:00", "23:59"));
    }

    private static List<OnboardingAvailabilitySlot> everydayInputs(int count) {
        return IntStream.range(0, count).mapToObj(index -> new OnboardingAvailabilitySlot("Everyday",
                LocalTime.MIDNIGHT.plusMinutes(index * 2L).toString(),
                LocalTime.MIDNIGHT.plusMinutes(index * 2L + 1).toString())).toList();
    }

    @Test
    void theProposedLimitAppliesToInputEntriesRatherThanExpandedRows() {
        var before = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4, everydayInputs(20), false));
        assertThat(before.availability()).hasSize(140);
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                everydayInputs(21), true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
                    assertThat(failure.getHttpStatus()).isEqualTo(400);
                    assertThat(failure.getMessage()).contains("20");
                });
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
    }

    @Test
    void springForwardGapCannotSilentlyChangeAnEnteredOnboardingTime() {
        clock.set(Instant.parse("2026-03-07T12:00:00Z"));
        jdbc.update("UPDATE users SET time_zone='America/Chicago' WHERE user_id='SID-T2'");
        var before = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), false));
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Sunday", "02:30", "04:00")), true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(400));
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
    }

    @Test
    void normalSlotsRoundTripAcrossTheFallTransitionAndUtcDayBoundary() {
        clock.set(Instant.parse("2026-10-31T12:00:00Z"));
        jdbc.update("UPDATE users SET time_zone='America/Chicago' WHERE user_id='SID-T2'");
        var saved = onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4, List.of(
                new OnboardingAvailabilitySlot("Everyday", "09:00", "10:00"),
                new OnboardingAvailabilitySlot("Saturday", "22:00", "23:00")), true));
        assertThat(onboarding.read("SID-T2")).isEqualTo(saved);
        assertThat(saved.availability()).hasSize(8);
        assertThat(saved.availability().stream().filter(slot -> slot.startTime().equals("09:00")))
                .hasSize(7).allSatisfy(slot -> assertThat(slot.endTime()).isEqualTo("10:00"));
        assertThat(saved.availability().stream().filter(slot -> slot.startTime().equals("22:00")))
                .extracting(OnboardingAvailabilitySlotResponse::dayOfWeek, OnboardingAvailabilitySlotResponse::endTime)
                .containsExactly(tuple("Saturday", "23:00"));
    }

    @Test
    void profileOvernightDataIsReadTruthfullyButCannotBeResavedUnderOnboardingRules() {
        var profile = profiles.updateAvailability(new UpdateAvailabilityRequest("SID-T2", "America/Chicago",
                List.of(new AvailabilitySlotRequest("Monday", "22:00", "02:00")), null, null, null));
        var before = onboarding.read("SID-T2");
        assertThat(before.availability()).extracting(OnboardingAvailabilitySlotResponse::dayOfWeek,
                OnboardingAvailabilitySlotResponse::startTime, OnboardingAvailabilitySlotResponse::endTime)
                .containsExactly(tuple("Monday", "22:00", "02:00"));
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "22:00", "02:00")), true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(400));
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
        assertThat(profiles.getAvailability("SID-T2")).isEqualTo(profile);
    }

    @Test
    void aDatabaseFailureAfterReplacementRollsBackSlotsEmergencyAndWizardProgress() {
        var profile = profiles.updateAvailability(new UpdateAvailabilityRequest("SID-T2", "Asia/Kolkata",
                List.of(new AvailabilitySlotRequest("Monday", "09:00", "12:00")), null, null, null));
        var before = onboarding.read("SID-T2");
        assertThat(before.promotionWizardStage()).isEqualTo(3);
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION reject_onboarding_state() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'synthetic state persistence failure'; END $$
                """);
        jdbc.execute("CREATE TRIGGER reject_onboarding_state AFTER UPDATE ON users FOR EACH ROW EXECUTE FUNCTION reject_onboarding_state()");
        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Tuesday", "14:00", "17:00")), true)))
                .isInstanceOf(DataAccessException.class);
        assertThat(onboarding.read("SID-T2")).isEqualTo(before);
        assertThat(profiles.getAvailability("SID-T2")).isEqualTo(profile);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void readingRequiresANonblankUserId(String userId) {
        assertThatThrownBy(() -> onboarding.read(userId))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(400));
        assertThat(onboarding.read("SID-T2").availability()).isEmpty();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void profileAndOnboardingWritersSerializeBeforeChoosingTheAccountTimezone() throws Exception {
        profiles.updateAvailability(new UpdateAvailabilityRequest("SID-T2", "UTC",
                List.of(new AvailabilitySlotRequest("Monday", "08:00", "09:00")), null, null, null));
        CountDownLatch profileLocked = new CountDownLatch(1);
        CountDownLatch onboardingAttemptedLock = new CountDownLatch(1);
        CountDownLatch releaseProfile = new CountDownLatch(1);
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            boolean accountLock = sql.stripTrailing().endsWith("FOR UPDATE");
            String threadName = Thread.currentThread().getName();
            if (accountLock && threadName.equals("ba33-onboarding-writer")) {
                onboardingAttemptedLock.countDown();
            }
            Object result = invocation.callRealMethod();
            if (accountLock && threadName.equals("ba33-profile-writer")) {
                profileLocked.countDown();
                if (!releaseProfile.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Concurrent onboarding writer never reached the account lock");
                }
            }
            return result;
        }).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var profileWrite = executor.submit(() -> {
                Thread.currentThread().setName("ba33-profile-writer");
                return profiles.updateAvailability(new UpdateAvailabilityRequest("SID-T2", "America/Chicago",
                        List.of(new AvailabilitySlotRequest("Monday", "09:00", "12:00")), null, null, null));
            });
            assertThat(profileLocked.await(10, TimeUnit.SECONDS)).isTrue();
            var onboardingWrite = executor.submit(() -> {
                Thread.currentThread().setName("ba33-onboarding-writer");
                return onboarding.save(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                        List.of(new OnboardingAvailabilitySlot("Tuesday", "14:00", "17:00")), true));
            });
            assertThat(onboardingAttemptedLock.await(10, TimeUnit.SECONDS)).isTrue();
            releaseProfile.countDown();
            assertThat(profileWrite.get(10, TimeUnit.SECONDS).getTimezone()).isEqualTo("America/Chicago");
            var saved = onboardingWrite.get(10, TimeUnit.SECONDS);
            assertThat(onboarding.read("SID-T2")).isEqualTo(saved);
            assertThat(saved.availability()).extracting(OnboardingAvailabilitySlotResponse::dayOfWeek,
                    OnboardingAvailabilitySlotResponse::startTime, OnboardingAvailabilitySlotResponse::endTime)
                    .containsExactly(tuple("Tuesday", "14:00", "17:00"));
            var finalProfile = profiles.getAvailability("SID-T2");
            assertThat(finalProfile.getTimezone()).isEqualTo("America/Chicago");
            assertThat(finalProfile.getSlots()).extracting(slot -> slot.getFrequency(), slot -> slot.getStartTime(),
                    slot -> slot.getEndTime()).containsExactly(tuple("Tuesday", "14:00", "17:00"));
        } finally {
            releaseProfile.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
