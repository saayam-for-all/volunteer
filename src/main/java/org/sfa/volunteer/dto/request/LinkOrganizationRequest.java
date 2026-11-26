package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LinkOrganizationRequest(
        @NotBlank(message = "is required") String userId,
        @NotBlank(message = "is required") String orgId
) {}