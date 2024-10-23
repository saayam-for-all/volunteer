package org.sfa.volunteer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "volunteer_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "volunteer_detail_id")
    private Long volunteerDetailId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "volunteer_details_user_id_fk"))
    private User user;

    @Column(name = "terms_and_conditions")
    private Boolean termsAndConditions;

    @Column(name = "terms_and_conditions_update_date")
    private ZonedDateTime termsAndConditionsUpdateDate;

    @Column(name = "govt_id")
    private String govtId;

    @Column(name = "govt_id_update_date")
    private ZonedDateTime govtIdUpdateDate;

    @Column(name = "pii")
    private String pii;

    @Column(name = "notification")
    private Boolean notification;

    @Column(name = "iscomplete")
    private Boolean isComplete;

    @Column(name = "completed_date")
    private ZonedDateTime completedDate;
}
