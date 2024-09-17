package org.sfa.volunteer.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.enums.RequestTypeEnum;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "RequestType")
@Table(
        name = "request_type",
        uniqueConstraints = {
                @UniqueConstraint(name = "request_type_id_unique", columnNames = "request_type_id")
        }
)
public class RequestType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_type_id", updatable = false)
    private Integer requestTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, columnDefinition = "VARCHAR(255)")
    private RequestTypeEnum type;

    @Column(name = "request_type_desc", columnDefinition = "VARCHAR(255)")
    private String description;

    @Column(name = "last_updated_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdatedAt;

    @JsonIgnore
    @OneToMany(
            mappedBy = "requestType",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<Request> requests = new HashSet<>();
}