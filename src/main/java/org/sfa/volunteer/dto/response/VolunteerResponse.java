package org.sfa.volunteer.dto.response;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

@Builder
public record VolunteerResponse(
        Integer id,
        @NotBlank String userId,
        Boolean termsAndConditions,
        ZonedDateTime tcUpdateDate,
        String govtIdFilename,
        ZonedDateTime govtUpdateDate,
        String skills,
        Boolean notification,
        Boolean isCompleted,
        ZonedDateTime completedDate,
        List<VolunteerUserAvailabilityResponse> availability) {
}
