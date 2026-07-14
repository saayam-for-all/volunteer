// MeetingResponse.java
package org.sfa.volunteer.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingResponse(
        Long id,
        String zoomMeetingId,
        String topic,
        String joinUrl,
        String startUrl,
        String password,
        LocalDateTime startTime,
        Integer durationMinutes,
        String hostUserId,
        List<String> attendeeEmails
) {}