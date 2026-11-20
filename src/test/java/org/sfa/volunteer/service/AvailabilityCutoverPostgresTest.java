package org.sfa.volunteer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySlot;
import org.sfa.volunteer.dto.request.AvailabilitySlotRequest;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.request.UserPreferenceRequest;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.service.impl.StewardServiceImpl;
import org.sfa.volunteer.service.impl.UserServiceImpl;
import org.sfa.volunteer.service.impl.VolunteerServiceImpl;
import org.sfa.volunteer.support.PostgresTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VolunteerServiceImpl.class, StewardServiceImpl.class, UserServiceImpl.class,
        AvailabilityCutoverPostgresTest.FixedClock.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfSystemProperty(named = "ba33.pg.enabled", matches = "true")
class AvailabilityCutoverPostgresTest {
    private static final String USER = "SID-T6";

    @TestConfiguration
    static class FixedClock {
        @Bean @Primary
        Clock cutoverClock() {
            return Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        PostgresTestDatabase.configure(properties, "ba33_t6");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired VolunteerService onboarding;
    @Autowired VolunteerService volunteers;
    @Autowired StewardService stewards;
    @Autowired UserService profiles;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void fixture() {
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS ba33_t6");
        jdbc.execute("DROP TABLE IF EXISTS user_availability, volunteer_details, user_additional_details, users, user_status");
        new ResourceDatabasePopulator(new ClassPathResource("availability214-schema.sql"),
                new ClassPathResource("ba33-cutover-schema.sql")).execute(dataSource);
        seedLegacyUser(USER);
    }

    private void seedLegacyUser(String id) {
        jdbc.update("""
                INSERT INTO users(user_id, full_name, time_zone, primary_email_address, promotion_wizard_stage, user_status_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, "Local Volunteer", "Asia/Kolkata", "cutover@example.test", 3, 1);
        jdbc.update("""
                INSERT INTO volunteer_details(user_id, availability_days, availability_times)
                VALUES (?, '["Friday"]'::jsonb, '[{"id":9,"dayOfWeek":"Friday","startTime":"10:00","endTime":"11:00"}]'::jsonb)
                """, id);
    }

    @Test
    void volunteerDetailsShowsNewRelationalScheduleInsteadOfOldJson() throws Exception {
        var saved = onboarding.updateVolunteerStep4(new OnboardingAvailabilitySaveRequest(USER, 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));

        var details = volunteers.getVolunteerByUserId(USER);

        assertThat(details.availabilityDays().toString()).isEqualTo("[\"Monday\"]");
        var slot = details.availabilityTimes().get(0);
        assertThat(details.availabilityTimes().size()).isOne();
        assertThat(slot.get("id").asInt()).isEqualTo(saved.availability().get(0).id());
        assertThat(slot.get("dayOfWeek").asText()).isEqualTo("Monday");
        assertThat(slot.get("startTime").asText()).isEqualTo("2026-07-06T03:30:00Z");
        assertThat(slot.get("endTime").asText()).isEqualTo("2026-07-06T06:30:00Z");
    }

    @Test
    void existingVolunteerAndStewardPagesExposeCanonicalAndLegacyOnlySchedules() throws Exception {
        String historical = "SID-T6-LEGACY";
        seedLegacyUser(historical);
        onboarding.updateVolunteerStep4(new OnboardingAvailabilitySaveRequest(USER, 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));

        var page = volunteers.findAllVolunteersWithPagination(0, 10);
        var stewardPage = stewards.getVolunteers(0, 10);
        ObjectMapper json = new ObjectMapper();

        var canonical = json.valueToTree(page.items().stream().filter(item -> USER.equals(item.userId())).findFirst().orElseThrow());
        var legacy = json.valueToTree(page.items().stream().filter(item -> historical.equals(item.userId())).findFirst().orElseThrow());
        assertThat(canonical.get("availabilityDays").toString()).isEqualTo("[\"Monday\"]");
        assertThat(canonical.get("availabilityMigrationRequired").asBoolean()).isFalse();
        assertThat(legacy.get("availabilityDays").toString()).isEqualTo("[\"Friday\"]");
        assertThat(legacy.get("availabilityTimes").get(0).get("startTime").asText()).isEqualTo("10:00");
        assertThat(legacy.get("availabilityMigrationRequired").asBoolean()).isTrue();
        var singleLegacy = json.valueToTree(volunteers.getVolunteerByUserId(historical));
        assertThat(singleLegacy).isEqualTo(legacy);

        var stewardCanonical = json.valueToTree(stewardPage.items().stream().filter(item -> USER.equals(item.userId())).findFirst().orElseThrow());
        var stewardLegacy = json.valueToTree(stewardPage.items().stream().filter(item -> historical.equals(item.userId())).findFirst().orElseThrow());
        assertThat(stewardCanonical.get("availabilityTimes")).isEqualTo(canonical.get("availabilityTimes"));
        assertThat(stewardCanonical.get("volunteerStage").asInt()).isEqualTo(4);
        assertThat(stewardLegacy.get("availabilityTimes")).isEqualTo(legacy.get("availabilityTimes"));
        assertThat(stewardLegacy.get("availabilityMigrationRequired").asBoolean()).isTrue();
    }

    @Test
    void canonicalReadsFlagLegacyOnlyScheduleUntilExplicitProfileClear() throws Exception {
        assertMigrationRequired(() -> onboarding.getOnboardingAvailability(USER));
        assertMigrationRequired(() -> profiles.getAvailability(USER));

        profiles.updateAvailability(new UpdateAvailabilityRequest(USER, "Asia/Kolkata", List.of(), null, null, null));

        assertThat(profiles.getAvailability(USER).getSlots()).isEmpty();
        assertThat(onboarding.getOnboardingAvailability(USER).availability()).isEmpty();
        assertThat(volunteers.getVolunteerByUserId(USER).availabilityMigrationRequired()).isFalse();
        assertThat(volunteers.getVolunteerByUserId(USER).availabilityTimes()).isNull();
    }

    private static void assertMigrationRequired(org.assertj.core.api.ThrowableAssert.ThrowingCallable read) {
        assertThatThrownBy(read).isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
            assertThat(failure.getHttpStatus()).isEqualTo(409);
            assertThat(failure.getErrorCode()).isEqualTo("LEGACY_AVAILABILITY_MIGRATION_REQUIRED");
        });
    }}
