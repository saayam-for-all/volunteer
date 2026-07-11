package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record NotificationRequest(
        @NotBlank String userId,
        Integer page,
        Integer size,
        LocalDateTime clientRefTime) {
}
