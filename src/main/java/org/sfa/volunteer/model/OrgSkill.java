package org.sfa.volunteer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "org_skills")
@IdClass(OrgSkillId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgSkill {
    @Id
    @Column(name = "org_id")
    private String orgId;

    @Id
    @Column(name = "cat_id")
    private String catId;

    @Column(name = "assigned_at")
    private ZonedDateTime assignedAt;
}