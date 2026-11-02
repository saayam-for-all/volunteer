package org.sfa.volunteer.dto.response;

import lombok.Builder;
import org.sfa.volunteer.enums.ActionStatus;

@Builder
public record NotificationActionResponse(
        Integer notificationId,
        String userId,
        ActionStatus actionStatus,
        String requestId
) {
    public NotificationActionResponse(Integer notificationId, String userId, ActionStatus actionStatus) {
        this(notificationId, userId, actionStatus, null);
    }
}
