package org.sfa.volunteer.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record GetNotificationsResponse(
                Integer totalCount,
                Integer newNotificationsCount,
                List<NotificationResponse> notifications) {
}
