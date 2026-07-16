package org.sfa.volunteer.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UserIdResponse(@NotBlank String user_id) {}
