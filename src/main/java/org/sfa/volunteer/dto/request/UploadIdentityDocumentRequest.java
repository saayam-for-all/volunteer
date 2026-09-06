package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UploadIdentityDocumentRequest(
        @NotBlank String userId,
        @NotNull @Min(1) @Max(2) Integer documentSlot,
        @NotBlank String documentName,
        @NotBlank String base64,
        @NotNull @Future LocalDate expiresOn,
        String regionHint) {}