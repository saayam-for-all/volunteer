package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpsertLastSeenRequest(
        @NotBlank String userId) {
}
