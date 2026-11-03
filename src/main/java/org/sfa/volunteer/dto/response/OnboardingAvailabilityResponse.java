package org.sfa.volunteer.dto.response;

import java.util.List;

public record OnboardingAvailabilityResponse(String userId, boolean isEmergencyAvailable,
        Integer promotionWizardStage, List<OnboardingAvailabilitySlotResponse> availability) {}
