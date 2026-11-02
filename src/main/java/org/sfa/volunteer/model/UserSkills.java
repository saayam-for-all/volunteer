package org.sfa.volunteer.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSkills {

    @EmbeddedId
    private UserSkillId id;

    // Postgres enum column; the cast is required since Hibernate 6.2 binds enums as varchar
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::skill_levels")
    @Column(name = "skill_level")
    private SkillLevel skillLevel;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at", insertable = false)
    private LocalDateTime lastUpdatedAt;

}
