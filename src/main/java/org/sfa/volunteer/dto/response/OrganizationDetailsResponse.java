package org.sfa.volunteer.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record OrganizationDetailsResponse(
        String orgId,
        String orgName,
        String orgType,
        String orgSize,
        String phone,
        String email,
        String webUrl,
        String street,
        String cityName,
        String stateId,
        String zipCode,
        String mission,
        List<String> categoryIds
) {
}