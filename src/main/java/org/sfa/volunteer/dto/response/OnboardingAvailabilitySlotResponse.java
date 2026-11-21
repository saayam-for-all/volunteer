package org.sfa.volunteer.dto.response;

public record OnboardingAvailabilitySlotResponse(Integer id, String dayOfWeek,
        String startTime, String endTime) {}
