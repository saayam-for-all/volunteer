package org.sfa.volunteer.dto.response;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
public record NotificationResponse(
                Integer notificationId,
                String status,
                String typeName,
                String message,
                Timestamp createDttm) {

        // Compact canonical constructor — JPQL can now instantiate the record
        public NotificationResponse {
        }
}
