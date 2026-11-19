package org.sfa.volunteer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.sfa.volunteer.aspect.LoggingAspect;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySlot;
import org.sfa.volunteer.dto.request.AvailabilitySlotRequest;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.response.OnboardingAvailabilitySlotResponse;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
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
@Import({org.sfa.volunteer.service.impl.VolunteerServiceImpl.class, UserServiceImpl.class,
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
        private final AtomicReference<Instant> afterNextRead = new AtomicReference<>();
        void set(Instant instant) {
            now.set(instant);
            afterNextRead.set(null);
        }
        void advanceAfterNextRead(Instant instant) { afterNextRead.set(instant); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() {
            Instant next = afterNextRead.getAndSet(null);
            return next == null ? now.get() : now.getAndSet(next);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        PostgresTestDatabase.configure(properties, "ba33_t2");
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET TIME ZONE 'America/Los_Angeles'");
    }

    @SpyBean JdbcTemplate jdbc;
    @Autowired VolunteerService onboarding;
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
        var saved = onboarding.updateVolunteerStep4(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
        var loaded = onboarding.getOnboardingAvailability("SID-T2");
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
        assertThatThrownBy(() -> onboarding.updateVolunteerStep4(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
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
        var before = onboarding.updateVolunteerStep4(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
        assertThatThrownBy(() -> onboarding.updateVolunteerStep4(request))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(400));
        assertThat(onboarding.getOnboardingAvailability("SID-T2")).isEqualTo(before);
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
        var before = onboarding.updateVolunteerStep4(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), false));
        assertThatThrownBy(() -> onboarding.updateVolunteerStep4(new OnboardingAvailabilitySaveRequest("SID-T2", 4,
                List.of(interval), true)))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(400));
        assertThat(onboarding.getOnboardingAvailability("SID-T2")).isEqualTo(before);
    }

}
