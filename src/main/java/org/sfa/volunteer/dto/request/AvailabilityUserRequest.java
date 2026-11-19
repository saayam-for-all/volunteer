package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AvailabilityUserRequest {
    @NotBlank
    private String userId;
}
