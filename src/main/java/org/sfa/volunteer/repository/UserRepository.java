package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    interface AvailabilityAccount {
        String getUserId();
        String getTimeZone();
        String getPrimaryEmailAddress();
        Integer getPromotionWizardStage();
    }

    String AVAILABILITY_ACCOUNT = """
            SELECT user_id AS "userId", time_zone AS "timeZone",
                   primary_email_address AS "primaryEmailAddress",
                   promotion_wizard_stage AS "promotionWizardStage"
            FROM users WHERE user_id = :userId
            """;

    @Query(value = AVAILABILITY_ACCOUNT, nativeQuery = true)
    Optional<AvailabilityAccount> findAvailabilityAccountById(@Param("userId") String userId);

    @Query(value = """
            SELECT user_id AS "userId", time_zone AS "timeZone",
                   primary_email_address AS "primaryEmailAddress",
                   promotion_wizard_stage AS "promotionWizardStage"
            FROM users WHERE user_id IN (:userIds)
            """, nativeQuery = true)
    List<AvailabilityAccount> findAvailabilityAccountsByIds(@Param("userIds") java.util.Collection<String> userIds);

    @Query(value = AVAILABILITY_ACCOUNT + " FOR UPDATE", nativeQuery = true)
    Optional<AvailabilityAccount> lockAvailabilityAccountById(@Param("userId") String userId);

    @Query(value = "SELECT is_emergency_available FROM users WHERE user_id = :userId", nativeQuery = true)
    boolean isEmergencyAvailable(@Param("userId") String userId);

    @Query(value = """
            SELECT data_type FROM information_schema.columns WHERE table_schema = current_schema()
              AND table_name = 'users' AND column_name = 'promotion_wizard_last_updated_at'
            """, nativeQuery = true)
    String wizardProgressTimestampType();

    // Both deployed timestamp layouts must preserve the UTC instant even with a non-UTC session timezone.
    default void saveOnboardingAvailabilityState(String userId, boolean emergency, java.time.LocalDateTime updatedAt) {
        var instant = updatedAt.atOffset(java.time.ZoneOffset.UTC);
        if ("timestamp with time zone".equals(wizardProgressTimestampType())) {
            saveOnboardingStateWithTimezone(userId, emergency, instant);
        } else {
            saveOnboardingStateWithoutTimezone(userId, emergency, instant);
        }
    }

    @Modifying
    @Query(value = """
            UPDATE users SET is_emergency_available = :emergency,
              promotion_wizard_stage = GREATEST(COALESCE(promotion_wizard_stage, 0), 4),
              promotion_wizard_last_updated_at = CAST(:updatedAt AS timestamp with time zone)
            WHERE user_id = :userId
            """, nativeQuery = true)
    void saveOnboardingStateWithTimezone(@Param("userId") String userId, @Param("emergency") boolean emergency,
            @Param("updatedAt") java.time.OffsetDateTime updatedAt);

    @Modifying
    @Query(value = """
            UPDATE users SET is_emergency_available = :emergency,
              promotion_wizard_stage = GREATEST(COALESCE(promotion_wizard_stage, 0), 4),
              promotion_wizard_last_updated_at = (CAST(:updatedAt AS timestamp with time zone) AT TIME ZONE 'UTC')
            WHERE user_id = :userId
            """, nativeQuery = true)
    void saveOnboardingStateWithoutTimezone(@Param("userId") String userId, @Param("emergency") boolean emergency,
            @Param("updatedAt") java.time.OffsetDateTime updatedAt);

    default void advanceWizardProgress(String userId, int step, java.time.LocalDateTime updatedAt) {
        var instant = updatedAt.atOffset(java.time.ZoneOffset.UTC);
        if ("timestamp with time zone".equals(wizardProgressTimestampType())) {
            advanceWizardProgressWithTimezone(userId, step, instant);
        } else {
            advanceWizardProgressWithoutTimezone(userId, step, instant);
        }
    }

    @Modifying
    @Query(value = """
            UPDATE users SET promotion_wizard_stage = :step,
              promotion_wizard_last_updated_at = CAST(:updatedAt AS timestamp with time zone)
            WHERE user_id = :userId AND COALESCE(promotion_wizard_stage, 0) < :step
            """, nativeQuery = true)
    void advanceWizardProgressWithTimezone(@Param("userId") String userId, @Param("step") int step,
            @Param("updatedAt") java.time.OffsetDateTime updatedAt);

    @Modifying
    @Query(value = """
            UPDATE users SET promotion_wizard_stage = :step,
              promotion_wizard_last_updated_at = (CAST(:updatedAt AS timestamp with time zone) AT TIME ZONE 'UTC')
            WHERE user_id = :userId AND COALESCE(promotion_wizard_stage, 0) < :step
            """, nativeQuery = true)
    void advanceWizardProgressWithoutTimezone(@Param("userId") String userId, @Param("step") int step,
            @Param("updatedAt") java.time.OffsetDateTime updatedAt);

    List<User> findByPrimaryEmailAddress(String email);

    List<User> findFirstByPrimaryEmailAddressIgnoreCase(String email);

    Optional<User> findFirstByPrimaryEmailAddressOrderByLastUpdateDateDesc(String email);
    // fallback if lastUpdateDate is null/old data
    Optional<User> findFirstByPrimaryEmailAddressOrderByIdDesc(String email);

    // Auth check for the profile-image path: confirms a *specific* user id
    // owns a given email, rather than resolving "the" user for an email (which
    // is ambiguous when two rows share an email -- see issue #165 follow-up on
    // duplicate-email rows). The caller already supplies the user id it
    // believes is correct; this just verifies that claim against this one row.
    boolean existsByIdAndPrimaryEmailAddressIgnoreCase(String id, String primaryEmailAddress);

    // Narrow read/write for the profile-image path: avoid hydrating or
    // saving the full User entity, which currently includes columns
    // (language_1/2/3, user_category_id) that no longer match the real
    // schema. See issue #165 follow-up on the broader schema drift.
    @Query("select u.profilePicturePath from User u where u.id = :userId")
    Optional<String> findProfilePicturePathById(@Param("userId") String userId);

    @Modifying
    @Query("update User u set u.profilePicturePath = :s3Uri, u.lastUpdateDate = :now where u.id = :userId")
    int updateProfilePicturePath(@Param("userId") String userId, @Param("s3Uri") String s3Uri,
            @Param("now") ZonedDateTime now);

    // Availability must not save unrelated User columns whose mappings may have schema drift.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.timeZone = :timezone where u.id = :userId")
    int updateAvailabilityTimezone(@Param("userId") String userId, @Param("timezone") String timezone);

    @Query("""
            select u from User u
            where
              lower(coalesce(u.fullName, '')) like :q
              or lower(coalesce(u.primaryEmailAddress, '')) like :q
              or coalesce(u.primaryPhoneNumber, '') like :q
              or lower(concat(coalesce(u.firstName, ''), ' ', coalesce(u.lastName, ''))) like :q
              or lower(concat(coalesce(u.lastName, ''), ' ', coalesce(u.firstName, ''))) like :q
            """)
    Page<User> searchUsers(@Param("q") String q, Pageable pageable);
}
