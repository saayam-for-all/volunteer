package org.sfa.volunteer.dto;

import lombok.Builder;

@Builder
public record UpdateUserProfileRequest(
        String firstName,
        String middleName,
        String lastName,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String cityName,
        String zipCode) {
}
