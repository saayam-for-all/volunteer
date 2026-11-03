package org.sfa.volunteer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sfa.volunteer.config.AvailabilityConfiguration;
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
import org.sfa.volunteer.repository.AvailabilityAccountRepository;
import org.sfa.volunteer.repository.LegacyAvailabilityRepository;
import org.sfa.volunteer.repository.OnboardingAvailabilityRepository;
import org.sfa.volunteer.repository.OnboardingAvailabilitySchema;
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
@Import({OnboardingAvailabilityService.class, AvailabilityScheduleService.class,
        AvailabilityAccountRepository.class, LegacyAvailabilityRepository.class, OnboardingAvailabilityRepository.class,
        OnboardingAvailabilitySchema.class, AvailabilityConfiguration.class,
        VolunteerServiceImpl.class, StewardServiceImpl.class, UserServiceImpl.class, LegacyAvailabilityProjection.class,
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
    @Autowired OnboardingAvailabilityService onboarding;
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
        var saved = onboarding.save(new OnboardingAvailabilitySaveRequest(USER, 4,
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
        onboarding.save(new OnboardingAvailabilitySaveRequest(USER, 4,
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
        assertMigrationRequired(() -> onboarding.read(USER));
        assertMigrationRequired(() -> profiles.getAvailability(USER));

        profiles.updateAvailability(new UpdateAvailabilityRequest(USER, "Asia/Kolkata", List.of(), null, null, null));

        assertThat(profiles.getAvailability(USER).getSlots()).isEmpty();
        assertThat(onboarding.read(USER).availability()).isEmpty();
        assertThat(volunteers.getVolunteerByUserId(USER).availabilityMigrationRequired()).isFalse();
        assertThat(volunteers.getVolunteerByUserId(USER).availabilityTimes()).isNull();
    }

    @Test
    void obsoleteInternalStepFourCannotWriteLegacyJsonOrAdvanceProgress() {
        var obsolete = VolunteerRequest.builder().userId(USER).step(4).availability(List.of()).build();
        assertThatThrownBy(() -> volunteers.updateVolunteer(obsolete))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getHttpStatus()).isEqualTo(400));
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_stage FROM users", Integer.class)).isEqualTo(3);
        assertMigrationRequired(() -> onboarding.read(USER));
    }

    @Test
    void staleIdentityDocumentWriterCannotRestoreClearedLegacyJson() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(outer -> {
            var stale = entityManager.find(Volunteer.class, USER);
            assertThat(stale.getAvailabilityDays().get(0).asText()).isEqualTo("Friday");
            independently(() -> profiles.updateAvailability(
                    new UpdateAvailabilityRequest(USER, "Asia/Kolkata", List.of(), null, null, null)));
            assertThatCode(() -> {
                volunteers.updateGovtIdPath(USER, 1, "s3://local-fixture/document");
                volunteers.updateGovtIdMetadata(USER, 1, "document.pdf", LocalDate.of(2027, 1, 1));
            }).doesNotThrowAnyException();
        });

        assertThat(profiles.getAvailability(USER).getSlots()).isEmpty();
        assertThat(volunteers.getVolunteerByUserId(USER).availabilityMigrationRequired()).isFalse();
        assertThat(volunteers.getGovtIdPath(USER, 1)).isEqualTo("s3://local-fixture/document");
        assertThat(volunteers.getIdentityDocumentMetadata(USER, 1).documentName()).isEqualTo("document.pdf");
    }

    @Test
    void failedOnboardingSaveRollsBackLegacyJsonCleanup() throws Exception {
        jdbc.execute("ALTER TABLE users ADD CONSTRAINT cutover_force_rollback CHECK (NOT is_emergency_available)");

        assertThatThrownBy(() -> onboarding.save(new OnboardingAvailabilitySaveRequest(USER, 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true)))
                .isInstanceOf(DataAccessException.class);

        assertMigrationRequired(() -> onboarding.read(USER));
        var legacy = volunteers.getVolunteerByUserId(USER);
        assertThat(legacy.availabilityMigrationRequired()).isTrue();
        assertThat(legacy.availabilityDays().get(0).asText()).isEqualTo("Friday");
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_stage FROM users", Integer.class)).isEqualTo(3);
    }

    @Test
    void staleEarlierStepEditsKeepNewerScheduleProgressAndOtherDocumentFields() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(outer -> {
            assertThat(entityManager.find(User.class, USER).getVolunteerStage()).isEqualTo(3);
            assertThat(entityManager.find(Volunteer.class, USER).getGovtIdPath2()).isNull();
            independently(() -> {
                onboarding.save(new OnboardingAvailabilitySaveRequest(USER, 4,
                        List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
                assertThatCode(() -> volunteers.updateGovtIdPath(USER, 2, "s3://local-fixture/current"))
                        .doesNotThrowAnyException();
            });

            assertThatCode(() -> {
                volunteers.updateVolunteerStep1(VolunteerRequest.builder().userId(USER).step(1)
                        .termsAndConditions(true).build());
                volunteers.updateVolunteerStep2(VolunteerRequest.builder().userId(USER).step(2)
                        .govtIdPath1("s3://local-fixture/added").build());
                volunteers.updateVolunteerStep3(VolunteerRequest.builder().userId(USER).step(3).build());
            }).doesNotThrowAnyException();
        });

        var current = onboarding.read(USER);
        assertThat(current.promotionWizardStage()).isEqualTo(4);
        assertThat(current.isEmergencyAvailable()).isTrue();
        assertThat(current.availability()).hasSize(1);
        var details = volunteers.getVolunteerByUserId(USER);
        assertThat(details.termsAndConditions()).isTrue();
        assertThat(details.govtIdPath1()).isEqualTo("s3://local-fixture/added");
        assertThat(details.govtIdPath2()).isEqualTo("s3://local-fixture/current");
    }

    @Test
    void profileRejectsClientProgressChangesAndAllowsMatchingValues() {
        saveOnboarding();
        var saved = profiles.getUserProfileById(USER);
        var changedStage = UpdateUserProfileRequest.builder().firstName("Should not save").volunteerStage(5).build();
        var changedTimestamp = UpdateUserProfileRequest.builder()
                .volunteerUpdateDate(saved.promotionWizardLastUpdateDate().plusMinutes(1)).build();
        assertThatThrownBy(() -> profiles.updateUserProfile(USER, changedStage)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profiles.updateUserProfile(USER, changedTimestamp)).isInstanceOf(IllegalArgumentException.class);
        assertThat(profiles.getUserProfileById(USER).firstName()).isNull();

        var matching = UpdateUserProfileRequest.builder().firstName("Updated").volunteerStage(4)
                .volunteerUpdateDate(saved.promotionWizardLastUpdateDate().withZoneSameInstant(ZoneId.of("Asia/Kolkata"))).build();
        var updated = profiles.updateUserProfile(USER, matching);
        assertThat(updated.firstName()).isEqualTo("Updated");
        assertThat(updated.promotionWizardStage()).isEqualTo(4);
        assertThat(updated.promotionWizardLastUpdateDate().toInstant())
                .isEqualTo(saved.promotionWizardLastUpdateDate().toInstant());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void staleProfileOrPreferencesWriteCannotRestoreOldProgress(boolean preferences) {
        new TransactionTemplate(transactions).executeWithoutResult(outer -> {
            assertThat(entityManager.find(User.class, USER).getVolunteerStage()).isEqualTo(3);
            independently(this::saveOnboarding);
            if (preferences) {
                assertThatCode(() -> profiles.updateUserPreferences(USER,
                        UserPreferenceRequest.builder().language1("en").secondaryEmail1("secondary@example.test").build()))
                        .doesNotThrowAnyException();
            } else {
                profiles.updateUserProfile(USER, UpdateUserProfileRequest.builder().firstName("Updated").build());
            }
        });

        assertThat(onboarding.read(USER).promotionWizardStage()).isEqualTo(4);
        var updated = profiles.getUserProfileById(USER);
        assertThat(updated.promotionWizardLastUpdateDate()).isNotNull();
        if (preferences) {
            assertThat(updated.language1()).isEqualTo("en");
            assertThat(jdbc.queryForObject("SELECT secondary_email_1 FROM user_additional_details", String.class))
                    .isEqualTo("secondary@example.test");
        } else {
            assertThat(updated.firstName()).isEqualTo("Updated");
        }
    }

    @Test
    void originalFirstThreeStepsStillAdvanceAndSaveTheirFields() throws Exception {
        jdbc.update("DELETE FROM volunteer_details WHERE user_id = ?", USER);
        jdbc.update("UPDATE users SET promotion_wizard_stage = NULL WHERE user_id = ?", USER);

        var created = volunteers.createVolunteer(VolunteerRequest.builder().userId(USER).step(1)
                .termsAndConditions(true).build());
        assertThat(created.termsAndConditions()).isTrue();
        assertThat(created.termsAcceptedAt()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_stage FROM users", Integer.class)).isEqualTo(1);

        var identity = volunteers.updateVolunteerStep2(VolunteerRequest.builder().userId(USER).step(2)
                .govtIdPath1("s3://local-fixture/identity").build());
        assertThat(identity.govtIdPath1()).isEqualTo("s3://local-fixture/identity");
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_stage FROM users", Integer.class)).isEqualTo(2);

        volunteers.updateVolunteerStep3(VolunteerRequest.builder().userId(USER).step(3).build());
        assertThat(onboarding.read(USER).promotionWizardStage()).isEqualTo(3);
    }

    @Test
    void reviewRequiresEligibleStoredIntervalsEvenAfterStepFourWasSaved() {
        saveOnboarding();
        var review = VolunteerRequest.builder().userId(USER).step(5).build();
        profiles.updateAvailability(new UpdateAvailabilityRequest(USER, "Asia/Kolkata",
                List.of(new AvailabilitySlotRequest("Monday", "22:00", "06:00")), null, null, null));
        assertThatThrownBy(() -> volunteers.updateVolunteerCompletion(review))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getErrorCode()).isEqualTo("STEP_FOUR_REQUIRED"));

        profiles.updateAvailability(new UpdateAvailabilityRequest(USER, "Asia/Kolkata", List.of(), null, null, null));
        assertThatThrownBy(() -> volunteers.updateVolunteerCompletion(review))
                .isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure ->
                        assertThat(failure.getErrorCode()).isEqualTo("STEP_FOUR_REQUIRED"));
        assertThat(onboarding.read(USER).promotionWizardStage()).isEqualTo(4);
    }

    private void saveOnboarding() {
        onboarding.save(new OnboardingAvailabilitySaveRequest(USER, 4,
                List.of(new OnboardingAvailabilitySlot("Monday", "09:00", "12:00")), true));
    }

    private void independently(Runnable operation) {
        TransactionTemplate separate = new TransactionTemplate(transactions);
        separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        separate.executeWithoutResult(ignored -> operation.run());
    }

    private static void assertMigrationRequired(org.assertj.core.api.ThrowableAssert.ThrowingCallable read) {
        assertThatThrownBy(read).isInstanceOfSatisfying(OnboardingAvailabilityException.class, failure -> {
            assertThat(failure.getHttpStatus()).isEqualTo(409);
            assertThat(failure.getErrorCode()).isEqualTo("LEGACY_AVAILABILITY_MIGRATION_REQUIRED");
        });
    }
}
