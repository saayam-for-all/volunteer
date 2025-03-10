package org.sfa.volunteer.dto.request;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record UpdateUserProfileRequest(
        String firstName,
        String middleName,
        String lastName,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String cityName,
        String zipCode,
        String profilePicturePath,
        Integer volunteerStage,
        ZonedDateTime volunteerUpdateDate) {
}
