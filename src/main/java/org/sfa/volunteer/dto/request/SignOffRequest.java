package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SignOffRequest( @NotBlank(message = "userId is required")
                              String userId,
                              String reason) {
}
