package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.SaveUserSkillsRequest;
import org.sfa.volunteer.dto.response.SaveUserSkillsResponse;

public interface SkillsService {
    SaveUserSkillsResponse saveUserSkills(SaveUserSkillsRequest request);
}
