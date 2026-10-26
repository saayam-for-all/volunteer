package org.sfa.volunteer.repository;

import java.util.List;

import org.sfa.volunteer.entities.UserNotificationPreference;
import org.sfa.volunteer.repository.projection.NotificationPreferenceRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes a user's per-channel notification preferences.
 *
 * <p>All queries are native because {@code user_notification_preferences.preference}
 * is a PostgreSQL enum ({@code preference_type}) and needs an explicit cast on write.
 */
@Repository
public interface UserNotificationPreferenceRepository
        extends JpaRepository<UserNotificationPreference, Long> {

    /**
     * Every known channel, with this user's preference where one has been recorded.
     *
     * <p>A LEFT JOIN from {@code notification_channels} so the caller always sees the
     * full set of channels. A channel the user has never touched comes back with a
     * null preference, which the API reports as "not set" rather than omitting it.
     */
    @Query(value = """
            SELECT c.channel_id       AS "channelId",
                   c.channel_name     AS "channelName",
                   c.description      AS "description",
                   CAST(p.preference AS text) AS "preference"
            FROM notification_channels c
            LEFT JOIN user_notification_preferences p
                   ON p.channel_id = c.channel_id
                  AND p.user_id = :userId
            ORDER BY c.channel_id
            """, nativeQuery = true)
    List<NotificationPreferenceRow> findPreferencesForUser(@Param("userId") String userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM user_notification_preferences
            WHERE user_id = :userId AND channel_id = :channelId
            """, nativeQuery = true)
    int countByUserAndChannel(@Param("userId") String userId,
                              @Param("channelId") Integer channelId);

    @Modifying
    @Query(value = """
            UPDATE user_notification_preferences
            SET preference = CAST(:preference AS preference_type)
            WHERE user_id = :userId AND channel_id = :channelId
            """, nativeQuery = true)
    int updatePreference(@Param("userId") String userId,
                         @Param("channelId") Integer channelId,
                         @Param("preference") String preference);

    @Modifying
    @Query(value = """
            INSERT INTO user_notification_preferences (user_id, channel_id, preference)
            VALUES (:userId, :channelId, CAST(:preference AS preference_type))
            """, nativeQuery = true)
    int insertPreference(@Param("userId") String userId,
                         @Param("channelId") Integer channelId,
                         @Param("preference") String preference);

    @Query(value = "SELECT COUNT(*) FROM notification_channels WHERE channel_id = :channelId",
            nativeQuery = true)
    int channelExists(@Param("channelId") Integer channelId);
}
