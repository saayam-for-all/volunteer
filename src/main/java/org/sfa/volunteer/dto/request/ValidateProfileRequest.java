package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ValidateProfileRequest(
        @NotBlank(message = "User ID is required") String userId
) {
}