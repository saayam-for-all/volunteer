package org.sfa.volunteer.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "day_of_week")
    private String dayOfWeek;

    @Column(name = "start_time")
    private ZonedDateTime startTime;

    @Column(name = "end_time")
    private ZonedDateTime endTime;

    @Column(name = "last_update_date")
    private ZonedDateTime lastUpdateDate;

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