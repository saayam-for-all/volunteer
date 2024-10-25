package org.sfa.volunteer.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record VolunteerUserAvailabilityResponse(
        Integer id,
        @NotBlank String userId,
        String dayOfWeek,
        ZonedDateTime startTime,
        ZonedDateTime endTime,
        ZonedDateTime lastUpdateDate) {
}
