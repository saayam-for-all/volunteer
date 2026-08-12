package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ProfileRequest(
    @NotBlank(message = "User ID cannot be blank") 
    String userId
) {
}