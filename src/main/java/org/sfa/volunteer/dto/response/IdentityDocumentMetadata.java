package org.sfa.volunteer.dto.response;

import org.sfa.volunteer.model.DocumentStatus;

import java.time.LocalDate;

public record IdentityDocumentMetadata(
        String documentName,
        LocalDate expiresOn,
        DocumentStatus status
) {}