package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record FindUserProfileUsingEmail(@NotBlank @Email String email) {
}
