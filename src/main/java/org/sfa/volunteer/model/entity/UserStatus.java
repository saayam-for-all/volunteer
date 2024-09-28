package org.sfa.volunteer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.enums.UserStatusEnum;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "user_status",
        uniqueConstraints = {
                @UniqueConstraint(name = "user_status_id_unique", columnNames = "user_status_id")
        }
)
public class UserStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_status_id")
    private Integer userStatusId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, columnDefinition = "VARCHAR(255)")
    private UserStatusEnum userStatus;

    @Column(name = "user_status_desc")
    private String userStatusDesc;

    @Column(name = "last_update_date")
    private ZonedDateTime lastUpdatedAt;
}
