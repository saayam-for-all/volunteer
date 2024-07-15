package org.sfa.volunteer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_status")
public class UserStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "user_status_id")
    private Integer userStatusId;

    @Column(name = "user_status", nullable = false)
    private String userStatus;

    @Column(name = "user_status_desc")
    private String userStatusDesc;

    @Column(name = "last_update_date")
    private ZonedDateTime lastUpdateDate;

}
