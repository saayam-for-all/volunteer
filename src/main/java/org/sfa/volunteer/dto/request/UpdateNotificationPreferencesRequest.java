package org.sfa.volunteer.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record UpdateNotificationPreferencesRequest(
        @NotBlank String userId,
        @NotEmpty @Valid List<NotificationPreferenceUpdate> preferences) {
}
