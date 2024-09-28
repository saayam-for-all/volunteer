package org.sfa.volunteer.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.enums.RequestPriorityEnum;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "RequestPriority")
@Table(
        name = "request_priority",
        uniqueConstraints = {
                @UniqueConstraint(name = "request_priority_id_unique", columnNames = "request_priority_id")
        }
)
public class RequestPriority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_priority_id", updatable = false)
    private Integer priorityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_priority", nullable = false, columnDefinition = "VARCHAR(255)")
    private RequestPriorityEnum priority;

    @Column(name = "request_priority_desc", columnDefinition = "VARCHAR(255)")
    private String description;

    @Column(name = "last_updated_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdatedAt;

    @JsonIgnore
    @OneToMany(
            mappedBy = "requestPriority",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<Request> requests = new HashSet<>();
}

