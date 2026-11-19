package org.sfa.volunteer.repository.projection;

/**
 * One channel and the calling user's preference for it, if any.
 */
public interface NotificationPreferenceRow {

    Integer getChannelId();

    String getChannelName();

    String getDescription();

    /** One of the {@code preference_type} values, or null when the user has not set one. */
    String getPreference();
}
