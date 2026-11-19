package org.sfa.volunteer.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record UpdateNotificationPreferencesResponse(
        String userId,
        Integer created,
        Integer updated,
        List<NotificationPreferenceResponse> preferences) {
}
