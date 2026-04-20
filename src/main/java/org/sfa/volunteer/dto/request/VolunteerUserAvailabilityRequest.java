package org.sfa.volunteer.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record VolunteerUserAvailabilityRequest(
        @JsonProperty("id")
        Integer id,
        @JsonProperty("dayOfWeek")
        @NotBlank(message = "Day of week is required" ) String dayOfWeek,
        @JsonProperty("startTime")
        @NotNull(message = "Start time is required") LocalTime startTime,
        @JsonProperty("endTime")
        @NotNull(message = "End time is required") LocalTime endTime) {
}
