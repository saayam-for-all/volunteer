package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record VolunteerUserAvailabilityRequest(
        @NotBlank String userId,
        String dayOfWeek,
        ZonedDateTime startTime,
        ZonedDateTime endTime,
        ZonedDateTime lastUpdateDate) {
}
