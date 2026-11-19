package org.sfa.volunteer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotResponse {
    // smallest user_availability_id among the rows this slot was built from
    private Integer id;
    private String frequency;
    // "HH:mm" in the user's local timezone
    private String startTime;
    private String endTime;
}
