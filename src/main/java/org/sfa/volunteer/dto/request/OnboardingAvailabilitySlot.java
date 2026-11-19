package org.sfa.volunteer.dto.request;

/** A weekly interval expressed in the account's local time. */
public record OnboardingAvailabilitySlot(String dayOfWeek, String startTime, String endTime) {}
