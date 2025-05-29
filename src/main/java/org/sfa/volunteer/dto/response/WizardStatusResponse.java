package org.sfa.volunteer.dto.response;

public record WizardStatusResponse(
    String userId,
    Integer promotion_wizard_stage,
    String addressAvailable
) {}
