package org.sfa.volunteer.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_availability")

public class VolunteerUserAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "user_availability_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "day_of_week",nullable = false,length = 10)
    private String dayOfWeek;

    @Column(name = "start_time",nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @Override
    public String toString() {
        return "VolunteerUserAvailability {" +
                "id=" + id +
                ", user='" + user + '\'' +
                ", dayOfWeek='" + dayOfWeek +'\'' +
                ", startTime=" + startTime +
                ", endTime='" + endTime + '\'' +
                ", lastUpdateDate=" + lastUpdateDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VolunteerUserAvailability volunteerAvail = (VolunteerUserAvailability) o;
        return Objects.equals(id, volunteerAvail.id) &&
                Objects.equals(user, volunteerAvail.user) &&
                Objects.equals(dayOfWeek, volunteerAvail.dayOfWeek) &&
                Objects.equals(startTime, volunteerAvail.startTime) &&
                Objects.equals(endTime, volunteerAvail.endTime) &&
                Objects.equals(lastUpdateDate, volunteerAvail.lastUpdateDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, dayOfWeek, startTime, endTime, lastUpdateDate);
    }
}