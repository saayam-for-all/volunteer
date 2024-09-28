package org.sfa.volunteer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.enums.UserCategoryEnum;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "user_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "user_category_id_unique", columnNames = "user_category_id")
        }
)
public class UserCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_category_id")
    private Integer userCategoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_category", nullable = false, columnDefinition = "VARCHAR(255)")
    private UserCategoryEnum userCategory;

    @Column(name = "user_category_desc", columnDefinition = "VARCHAR(255)")
    private String userCategoryDesc;

    @Column(name = "last_update_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdateDate;
}
