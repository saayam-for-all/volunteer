package org.sfa.volunteer.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record VolunteerResponse(
        String id,
        String userId,
        Boolean termsAndConditions,
        LocalDateTime termsAcceptedAt,
        String govtIdPath1,
        String govtIdPath2,
        LocalDateTime path1UpdatedAt,
        LocalDateTime path2UpdatedAt,
        JsonNode availabilityDays,
        JsonNode availabilityTimes,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt) {
}