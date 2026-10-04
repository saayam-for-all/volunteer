// language: java
package org.sfa.volunteer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "user_skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVolunteerSkill {

    @EmbeddedId
    private UserVolunteerSkillId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // cat_id is part of the embedded id; helper getter/setter if convenient
    public String getCatId() {
        return id != null ? id.getCatId() : null;
    }

    public void setCatId(String catId) {
        if (id == null) id = new UserVolunteerSkillId();
        id.setCatId(catId);
    }

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "last_updated_at")
    private ZonedDateTime lastUpdatedAt;
}
