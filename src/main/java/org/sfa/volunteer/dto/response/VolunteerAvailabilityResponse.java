package org.sfa.volunteer.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record VolunteerAvailabilityResponse(

        @JsonProperty("userId")
        String userId,

        @JsonProperty("isEmergencyAvailable")
        boolean isEmergencyAvailable,

        @JsonProperty("totalSlots")
        int totalSlots,

        @JsonProperty("availability")
        List<VolunteerUserAvailabilityResponse> availability

) {}
