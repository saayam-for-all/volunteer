package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserOrganizationsRequest(
        @NotBlank(message = "is required") String userId
) {}