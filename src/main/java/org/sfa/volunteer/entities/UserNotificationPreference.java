package org.sfa.volunteer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A user's delivery preference for one notification channel.
 *
 * <p>Maps {@code user_notification_preferences}. The {@code preference} column is a
 * PostgreSQL enum ({@code preference_type}), so it is read and written through native
 * queries with an explicit cast rather than a JPA enum mapping.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_notification_preferences")
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_notification_preferences_id")
    private Long userNotificationPreferencesId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    @Column(name = "preference", insertable = false, updatable = false)
    private String preference;
}
