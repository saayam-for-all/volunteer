package org.sfa.volunteer.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.enums.RequestCategoryEnum;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "RequestCategory")
@Table(
        name = "request_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "request_category_id_unique", columnNames = "request_category_id")
        }
)
public class RequestCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_category_id", updatable = false)
    private Integer requestCategoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_category", nullable = false, columnDefinition = "VARCHAR(255)")
    private RequestCategoryEnum category;

    @Column(name = "request_category_desc", columnDefinition = "VARCHAR(255)")
    private String description;

    @Column(name = "last_updated_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdatedAt;

    @JsonIgnore
    @OneToMany(
            mappedBy = "requestCategory",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<Request> requests = new HashSet<>();
}

