package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * One channel's desired preference.
 *
 * @param channelId  a {@code notification_channels.channel_id}
 * @param preference one of email/text/both, or null to clear the preference
 */
@Builder
public record NotificationPreferenceUpdate(
        @NotNull Integer channelId,
        String preference) {
}
