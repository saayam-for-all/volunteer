package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvailabilityRequest {
    @NotBlank
    private String userId;
    // IANA zone, e.g. America/Chicago
    private String timezone;
    private List<AvailabilitySlotRequest> slots;

    // Sent by the webapp but not stored yet: user_availability has no vacation columns
    private Boolean vacationMode;
    private String vacationStartDate;
    private String vacationEndDate;
}
