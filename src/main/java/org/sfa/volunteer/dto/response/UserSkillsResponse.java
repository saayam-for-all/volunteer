package org.sfa.volunteer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sfa.volunteer.model.SkillLevel;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillsResponse {

    private String userId;
    // category ids, e.g. ["4.2", "0.1"]
    private List<String> skills;
    // category id -> skill level (null when not set)
    private Map<String, SkillLevel> skillLevels;
}
