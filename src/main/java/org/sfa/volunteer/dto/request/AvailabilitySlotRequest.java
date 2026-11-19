package org.sfa.volunteer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotRequest {
    // Everyday, Weekdays, Weekends or Monday .. Sunday
    private String frequency;
    // "HH:mm" in the user's local timezone
    private String startTime;
    private String endTime;
}
