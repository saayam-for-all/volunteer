package org.sfa.volunteer.dto.response;

import lombok.Builder;

/**
 * A channel and the user's delivery preference for it.
 *
 * @param preference one of email/text/both, or null when the user has not set one
 */
@Builder
public record NotificationPreferenceResponse(
        Integer channelId,
        String channelName,
        String description,
        String preference) {
}
