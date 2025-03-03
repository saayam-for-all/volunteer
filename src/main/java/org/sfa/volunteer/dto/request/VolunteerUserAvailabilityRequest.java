package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record VolunteerUserAvailabilityRequest(
        Integer id,
        @NotBlank String dayOfWeek,
        @NotBlank ZonedDateTime startTime,
        @NotBlank ZonedDateTime endTime,
        ZonedDateTime lastUpdateDate) {
}
