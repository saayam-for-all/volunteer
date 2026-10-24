package org.sfa.volunteer.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.ZonedDateTime;

// NOTE (#214): only used by the commented-out availability methods in VolunteerService(Impl); kept with them.
// The live endpoints return AvailabilityResponse / AvailabilitySlotResponse.
@Builder
public record VolunteerUserAvailabilityResponse(
        Integer id,
        @NotBlank String userId,
        String dayOfWeek,
        ZonedDateTime startTime,
        ZonedDateTime endTime,
        ZonedDateTime lastUpdateDate) {
}
