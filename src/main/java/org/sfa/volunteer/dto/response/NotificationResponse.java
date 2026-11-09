package org.sfa.volunteer.dto.response;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.sfa.volunteer.enums.ActionStatus;

@Builder
public record NotificationResponse(
                Integer notificationId,
                String status,
                String typeName,
                String message,
                Timestamp createDttm,
                String requestId,
                ActionStatus actionStatus) {

        // Compact canonical constructor — JPQL can now instantiate the record
        public NotificationResponse {
        }

        public NotificationResponse(Integer notificationId, String status, String typeName,
                                    String message, Timestamp createDttm) {
                this(notificationId, status, typeName, message, createDttm, null, null);
        }
}
