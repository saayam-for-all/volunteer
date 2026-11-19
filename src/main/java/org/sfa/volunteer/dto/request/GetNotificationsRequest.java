package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GetNotificationsRequest(
        @NotBlank String userId,
        Integer rowStart,
        Integer rowEnd) {
}