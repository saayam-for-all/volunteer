package org.sfa.volunteer.dto.request;

import java.util.List;

public record OnboardingAvailabilitySaveRequest(String userId, Integer step,
        List<OnboardingAvailabilitySlot> availability, Boolean isEmergencyAvailable) {}
