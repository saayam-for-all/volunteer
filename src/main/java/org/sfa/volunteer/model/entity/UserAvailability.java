package org.sfa.volunteer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.enums.DayOfWeekEnum;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_availability")
public class UserAvailability {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "day_of_week", nullable = false)
    @Enumerated(EnumType.STRING) // Assuming you want to use an enum for day_of_week
    private DayOfWeekEnum dayOfWeek;

    @Column(name = "start_time", columnDefinition = "TIMESTAMP", nullable = false)
    private ZonedDateTime startTime;

    @Column(name = "end_time", columnDefinition = "TIMESTAMP", nullable = false)
    private ZonedDateTime endTime;

    @Column(name = "last_update_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdateDate;
}

