package org.sfa.volunteer.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.List;

@Builder
public record VolunteerRequest(
        @NotBlank Integer step,
        @NotBlank String userId,
        Boolean termsAndConditions,
        String govtIdPath1,
        String govtIdPath2,
        List<VolunteerUserAvailabilityRequest> availability) {
}