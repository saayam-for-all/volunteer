package org.sfa.volunteer.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record SaveUserSkillsRequest(
        String userId,
        List<String> catIds
) {}
