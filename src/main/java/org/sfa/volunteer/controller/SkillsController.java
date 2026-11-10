package org.sfa.volunteer.controller;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.SaveUserSkillsRequest;
import org.sfa.volunteer.dto.response.SaveUserSkillsResponse;
import org.sfa.volunteer.service.SkillsService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/0.0.1/users")
public class SkillsController {

    private final SkillsService skillsService;
    private final ResponseBuilder responseBuilder;

    public SkillsController(SkillsService skillsService, ResponseBuilder responseBuilder) {
        this.skillsService = skillsService;
        this.responseBuilder = responseBuilder;
    }

    @PutMapping("/skills")
    public SaayamResponse<SaveUserSkillsResponse> saveUserSkills(
            @RequestBody SaveUserSkillsRequest request) {

        SaveUserSkillsResponse response = skillsService.saveUserSkills(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.USER_SKILLS_SAVED, response);
    }
}
