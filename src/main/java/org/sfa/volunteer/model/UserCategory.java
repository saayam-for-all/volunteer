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
@Table(name = "user_category")
public class UserCategory {

    @Id
    @Column(name = "user_category_id", nullable = false)
    private Integer userCategoryId;

    @Column(name = "user_category", length = 255)
    private String userCategory;

    @Column(name = "user_category_desc", length = 255)
    private String userCategoryDesc;

    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Column(name = "user_access_level")
    private Short userAccessLevel;

    @Column(name = "is_deprecated")
    private Boolean isDeprecated;

    @Column(name = "permissions", columnDefinition = "jsonb")
    private String permissions;

    @Column(name = "last_updated_at")
    private ZonedDateTime lastUpdatedAt;
}
