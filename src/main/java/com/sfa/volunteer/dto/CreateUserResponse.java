package com.sfa.volunteer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateUserResponse(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String phoneNumber,
        @NotBlank String timeZone,
        @NotBlank String userId
) {
}
