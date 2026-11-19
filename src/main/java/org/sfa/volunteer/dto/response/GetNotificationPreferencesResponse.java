package org.sfa.volunteer.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record GetNotificationPreferencesResponse(
        String userId,
        List<NotificationPreferenceResponse> preferences) {
}
