package org.sfa.volunteer.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillId implements Serializable {
    @Column(name = "user_id")
    private String userId;

    @Column(name = "cat_id")
    private String catId;
}
