package org.sfa.volunteer.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Builder
public record VolunteerUserAvailabilityResponse(
        @JsonProperty("id")
        Integer id,
        @JsonProperty("userId")
        String userId,
        @JsonProperty("dayOfWeek")
        String dayOfWeek,
        @JsonProperty("startTime")
        String startTime,
        @JsonProperty("endTime")
        String endTime,
        @JsonProperty("lastUpdateDate")
        LocalDateTime lastUpdateDate) {
}
