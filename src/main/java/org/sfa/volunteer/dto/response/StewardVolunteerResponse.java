package org.sfa.volunteer.dto.response;

// public class StewardVolunteerResponse {
    
// }

import lombok.Builder;

@Builder
public record StewardVolunteerResponse(
        String userId,
        Integer volunteerStage,
        Object availabilityDays,
        Object availabilityTimes
) {
}