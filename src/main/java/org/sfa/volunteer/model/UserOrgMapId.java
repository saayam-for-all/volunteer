package org.sfa.volunteer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrgMapId implements Serializable {
    private String userId;
    private String orgId;
}