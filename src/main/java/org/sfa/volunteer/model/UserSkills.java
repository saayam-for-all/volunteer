package org.sfa.volunteer.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

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

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at", insertable = false, updatable = false)
    private LocalDateTime lastUpdatedAt;

}
