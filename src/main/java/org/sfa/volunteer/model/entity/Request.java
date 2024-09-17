package org.sfa.volunteer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * ClassName: Request
 * Package: org.sfa.request.model.entity
 * Description:
 *
 * @author Fan Peng
 * Create 2024/6/13 22:38
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "request",
        uniqueConstraints = {
                @UniqueConstraint(name = "request_id_unique", columnNames = "request_id")
        }
)
public class Request implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id", updatable = false, columnDefinition = "VARCHAR(255)")
    private String requestId;

    @Column(name = "request_user_id", nullable = false, columnDefinition = "VARCHAR(255)")
    private String requesterId;

    @ManyToOne
    @JoinColumn(
            name = "request_status_id",
            nullable = false,
            referencedColumnName = "request_status_id",
            foreignKey = @ForeignKey(name = "fk_request_status_id")
    )
    private RequestStatus requestStatus;


    @ManyToOne
    @JoinColumn(
            name = "request_priority_id",
            nullable = false,
            referencedColumnName = "request_priority_id",
            foreignKey = @ForeignKey(name = "fk_request_priority_id")
    )
    private RequestPriority requestPriority;

    @ManyToOne
    @JoinColumn(
            name = "request_type_id",
            nullable = false,
            referencedColumnName = "request_type_id",
            foreignKey = @ForeignKey(name = "fk_request_type_id")
    )
    private RequestType requestType;

    @ManyToOne
    @JoinColumn(
            name = "request_category_id",
            nullable = false,
            referencedColumnName = "request_category_id",
            foreignKey = @ForeignKey(name = "fk_request_category_id")
    )
    private RequestCategory requestCategory;

    @ManyToOne
    @JoinColumn(
            name = "request_for_id",
            nullable = false,
            referencedColumnName = "request_for_id",
            foreignKey = @ForeignKey(name = "fk_request_for_id")
    )
    private RequestFor requestFor;

    @Column(name = "city_name", columnDefinition = "VARCHAR(255)")
    private String city;

    @Column(name = "zip_code", columnDefinition = "VARCHAR(20)")
    private String zipCode;

    @Column(name = "request_desc", nullable = false, columnDefinition = "VARCHAR(255)")
    private String requestDescription;

    @Column(name = "audio_req_desc", columnDefinition = "VARCHAR(255)")
    private String audioRequestDescription;

    @Column(name = "submission_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime submittedAt;

    @Column(name = "lead_volunteer_user_id")
    private Integer leadVolunteerUserId;

    @Column(name = "serviced_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime servicedAt;

    @Column(name = "last_update_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdatedAt;

    // Transient fields for formatted dates
    private transient String formattedSubmittedAt;
    private transient String formattedServicedAt;
    private transient String formattedLastUpdatedAt;
}