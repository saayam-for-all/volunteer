package org.sfa.volunteer.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One row of user_availability: a single weekly slot on one day.
 * day_of_week, start_time and end_time are all in UTC (DB wiki #32).
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_availability")
public class VolunteerUserAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_availability_id", nullable = false)
    private Integer id;

    // toString/equals/hashCode come from @Data and deliberately leave out this lazy association.
    // Including it, as the old hand-written overrides did, calls User.toString()/equals() on a
    // Hibernate proxy: an extra query, or a LazyInitializationException outside a transaction
    // (open-in-view is off). Rows are only kept in Lists, so nothing relies on comparing users.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    // "Monday" .. "Sunday", the UTC day of start_time
    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
}
