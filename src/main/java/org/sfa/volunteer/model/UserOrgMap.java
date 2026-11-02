package org.sfa.volunteer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_org_map")
@IdClass(UserOrgMapId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrgMap {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "org_id")
    private String orgId;

    @Column(name = "user_role")
    private String userRole;
}