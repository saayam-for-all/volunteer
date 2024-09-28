package org.sfa.volunteer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.enums.SkillLevelEnum;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_skills")
public class UserSkills {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_skills_id")
    private Integer userSkillsId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "skill_id", nullable = false)
    private Integer skillId;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level")
    private SkillLevelEnum skillLevel;

    @Column(name = "last_used_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUsedDate;

    @Column(name = "created_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime createdDate;

    @Column(name = "last_update_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdateDate;

    @ManyToOne
    @JoinColumn(name = "skill_id", insertable = false, updatable = false)
    private SkillList skillList;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

}
