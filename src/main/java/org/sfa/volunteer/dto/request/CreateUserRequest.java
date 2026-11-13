package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record   CreateUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phoneNumber,
        String timeZone,
        String country,
        String locale) {
}
