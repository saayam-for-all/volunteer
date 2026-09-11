package org.sfa.volunteer.dto.response;

import lombok.Builder;

@Builder
public record OrganizationResponse(
        String orgId,
        String orgName,
        String cityName,
        String stateId) {
}