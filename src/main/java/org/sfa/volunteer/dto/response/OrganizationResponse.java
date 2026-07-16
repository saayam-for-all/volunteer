package org.sfa.volunteer.dto.response;

import lombok.Builder;

@Builder
public record OrganizationResponse(
        Long id,
        String organizationName,
        String organizationType,
        String phoneNumber,
        String email,
        String url,
        String streetAddress1,
        String streetAddress2,
        String city,
        String state,
        String zipCode
) {
}
