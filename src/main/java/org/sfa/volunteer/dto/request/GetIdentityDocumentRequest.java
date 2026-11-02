package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GetIdentityDocumentRequest(
        @NotBlank String userId,
        @NotNull @Min(1) @Max(2) Integer documentSlot) {}