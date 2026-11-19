package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSkillsRequest {
    @NotBlank
    private String userId;

}
