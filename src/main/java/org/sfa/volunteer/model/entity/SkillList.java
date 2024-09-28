package org.sfa.volunteer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skill_list")
public class SkillList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_list_id")
    private Integer skillListId;

    @Column(name = "skill_desc", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String skillDesc;

    @Column(name = "last_update_date", columnDefinition = "TIMESTAMP")
    private ZonedDateTime lastUpdateDate;

    @Column(name = "request_category_id", nullable = false)
    private Integer requestCategoryId;
}

