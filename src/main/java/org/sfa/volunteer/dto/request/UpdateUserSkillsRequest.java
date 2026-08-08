package org.sfa.volunteer.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class UpdateUserSkillsRequest {

    private String userId;
    private List<String> skills;

}