package org.sfa.volunteer.dto.response;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record VolunteerResponse(
        Long volunteerDetailId,
        String userId,
        Boolean termsAndConditions,
        ZonedDateTime termsAndConditionsUpdateDate,
        String govtId,
        ZonedDateTime govtIdUpdateDate,
        String pii,
        Boolean notification,
        Boolean isComplete,
        ZonedDateTime completedDate) {
}
