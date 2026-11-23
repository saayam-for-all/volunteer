package org.sfa.volunteer.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record SaveUserSkillsResponse(
        String userId,
        List<String> savedCatIds,
        Integer wizardStage
) {}
