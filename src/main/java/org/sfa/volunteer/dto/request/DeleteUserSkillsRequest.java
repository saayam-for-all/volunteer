package org.sfa.volunteer.dto.request;

import lombok.*;
import java.util.*;

@Data
public class DeleteUserSkillsRequest {
    private String userId;
    private List<String> skills;
}
