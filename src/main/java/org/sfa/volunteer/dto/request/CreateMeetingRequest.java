// CreateMeetingRequest.java
package org.sfa.volunteer.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record CreateMeetingRequest(
        String topic,
        LocalDateTime startTime,
        Integer durationMinutes,
        String hostUserId,           // Volunteer creating the meeting
        List<String> attendeeEmails  // Users being invited
) {}