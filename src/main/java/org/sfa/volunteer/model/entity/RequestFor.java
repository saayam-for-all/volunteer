package org.sfa.volunteer.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.enums.RequestForEnum;


import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "RequestFor")
@Table(
        name = "request_for",
        uniqueConstraints = {
                @UniqueConstraint(name = "request_for_id_unique", columnNames = "request_for_id")
        }
)
public class RequestFor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_for_id", updatable = false)
    private Integer requestForId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_for", nullable = false, columnDefinition = "VARCHAR(255)")
    private RequestForEnum For;

    @Column(name = "request_for_desc", columnDefinition = "VARCHAR(255)")
    private String description;

    @Column(name = "last_updated_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdatedAt;

    @JsonIgnore
    @OneToMany(
            mappedBy = "requestFor",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<Request> requests = new HashSet<>();
}

