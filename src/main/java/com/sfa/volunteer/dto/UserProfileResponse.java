package com.sfa.volunteer.dto;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record UserProfileResponse(
        String id,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        String emailAddress,
        String phoneNumber,
        String timeZone,
        String profilePicturePath,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String city,
        String zipCode,
        ZonedDateTime lastUpdateDate,
        String stateName,
        String countryName,
        String userStatus,
        String userCategory) {
}