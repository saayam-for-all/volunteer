package org.sfa.volunteer.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
@Table(name = "volunteer_details")

public class Volunteer {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "volunteer_detail_id", nullable = false)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "terms_and_conditions")
    private Boolean termsAndConditions;

    @Column(name = "terms_and_conditions_update_date")
    private ZonedDateTime tcUpdateDate;

    @Column(name = "govt_id")
    private String govtIdFilename;

    @Column(name = "govt_id_update_date")
    private ZonedDateTime govtUpdateDate;

    @Column(name = "skills")
    private String skills;

    @Column(name = "notification")
    private Boolean notification;

    @Column(name = "iscomplete")
    private Boolean isCompleted;

    @Column(name = "completed_date")
    private ZonedDateTime completedDate;

    @Override
    public String toString() {
        return "Volunteer {" +
                "id=" + id +
                ", user='" + user + '\'' +
                ", termsAndConditions=" + termsAndConditions +
                ", tcUpdateDate=" + tcUpdateDate +
                ", govtIdFilename='" + govtIdFilename + '\'' +
                ", govtUpdateDate=" + govtUpdateDate +
                ", skills='" + skills + '\'' +
                ", notification=" + notification +
                ", isCompleted=" + isCompleted +
                ", completedDate=" + completedDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Volunteer volunteer = (Volunteer) o;
        return Objects.equals(id, volunteer.id) &&
                Objects.equals(user, volunteer.user) &&
                Objects.equals(termsAndConditions, volunteer.termsAndConditions) &&
                Objects.equals(tcUpdateDate, volunteer.tcUpdateDate) &&
                Objects.equals(govtIdFilename, volunteer.govtIdFilename) &&
                Objects.equals(govtUpdateDate, volunteer.govtUpdateDate) &&
                Objects.equals(skills, volunteer.skills) &&
                Objects.equals(notification, volunteer.notification) &&
                Objects.equals(isCompleted, volunteer.isCompleted) &&
                Objects.equals(completedDate, volunteer.completedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, termsAndConditions, tcUpdateDate, govtIdFilename, govtUpdateDate, skills, notification, isCompleted, completedDate );
    }
}