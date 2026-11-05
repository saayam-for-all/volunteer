package org.sfa.volunteer.dto.request;

import lombok.Builder;

@Builder
public record UpdateProfilePageRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String countryName) {
}
