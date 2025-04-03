package org.sfa.volunteer.dto.response;

import lombok.Builder;

@Builder
public record AdditionalUserInfoResponse(
        Long additionalDetailId,
        String secondaryEmail1,
        String secondaryEmail2,
        String secondaryPhone1,
        String secondaryPhone2,
        String userId) {
}
