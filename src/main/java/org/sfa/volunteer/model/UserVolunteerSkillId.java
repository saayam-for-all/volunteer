package org.sfa.volunteer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVolunteerSkillId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "cat_id", nullable = false)
    private String catId;
}
