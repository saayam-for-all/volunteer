package org.sfa.volunteer.dto.response;

import lombok.Builder;
import org.sfa.volunteer.enums.ActionStatus;

@Builder
public record NotificationActionResponse(
        Integer notificationId,
        String userId,
        ActionStatus actionStatus
) {}
