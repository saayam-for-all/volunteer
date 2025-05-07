package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateOrganizationRequest(
        @NotBlank String organizationName,
        @NotBlank String organizationType,
        @NotBlank String phoneNumber,
        @NotBlank String email,
        String url,
        @NotBlank String streetAddress1,
        String streetAddress2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode
) {
}
