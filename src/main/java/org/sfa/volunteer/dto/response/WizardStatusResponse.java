package org.sfa.volunteer.dto.response;

public record WizardStatusResponse(
    String userId,
    Integer promotionWizardStage,
    String addressAvailable
) {}
