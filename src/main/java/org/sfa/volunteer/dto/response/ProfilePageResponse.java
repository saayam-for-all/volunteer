package org.sfa.volunteer.dto.response;

import lombok.Builder;

@Builder
public record ProfilePageResponse(
        String userId,
        String firstName,
        String lastName,
        String emailAddress,
        String phoneNumber,
        String countryName) {
}
