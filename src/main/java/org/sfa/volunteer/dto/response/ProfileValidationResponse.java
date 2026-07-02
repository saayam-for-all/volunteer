package org.sfa.volunteer.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ProfileValidationResponse(
        boolean profileComplete,
        String userId,
        List<String> missingFields
) {
}