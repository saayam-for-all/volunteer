package org.sfa.volunteer.util;

import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.util.AvailabilityTimeConverter.LocalSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Onboarding rules are separate from Profile's empty/overnight/deduplication policy. */
public final class OnboardingAvailabilityPolicy {
    // Proposed operational bound inherited from Profile; product confirmation remains a release gate.
    private static final int MAX_INPUT_SLOTS = 20;
    private static final Pattern LOCAL_MINUTES = Pattern.compile("(?:[01][0-9]|2[0-3]):[0-5][0-9]");
    private OnboardingAvailabilityPolicy() {}

    public static List<LocalSlot> normalize(OnboardingAvailabilitySaveRequest request) {
        if (request == null) {
            throw badRequest("Request body is required");
        }
        requireUserId(request.userId());
        if (!Integer.valueOf(4).equals(request.step())) {
            throw badRequest("step must be 4");
        }
        if (request.isEmergencyAvailable() == null) {
            throw badRequest("isEmergencyAvailable must be an explicit boolean");
        }
        if (request.availability() == null || request.availability().isEmpty()) {
            throw badRequest("At least one availability interval is required");
        }
        if (request.availability().size() > MAX_INPUT_SLOTS) {
            throw badRequest("availability must contain at most 20 input intervals");
        }
        List<LocalSlot> slots = new ArrayList<>();
        for (var slot : request.availability()) {
            if (slot == null) {
                throw badRequest("Each availability interval is required");
            }
            LocalTime start = parseTime(slot.startTime());
            LocalTime end = parseTime(slot.endTime());
            if (!end.isAfter(start)) {
                throw badRequest("endTime must be later than startTime on the same day");
            }
            for (DayOfWeek day : parseDays(slot.dayOfWeek())) {
                slots.add(new LocalSlot(day, start, end));
            }
        }
        if (hasOverlap(slots)) throw badRequest("Availability intervals must not overlap on the same day");
        return List.copyOf(slots);
    }

    public static boolean hasOverlap(List<LocalSlot> slots) {
        for (int first = 0; first < slots.size(); first++) {
            for (int second = first + 1; second < slots.size(); second++) {
                if (AvailabilityTimeConverter.overlaps(slots.get(first), slots.get(second))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw badRequest("userId is required");
        }
    }

    private static Set<DayOfWeek> parseDays(String value) {
        if ("Everyday".equalsIgnoreCase(value)) {
            return EnumSet.allOf(DayOfWeek.class);
        }
        if (value != null) {
            try {
                return EnumSet.of(DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Group labels other than Everyday are not onboarding choices.
            }
        }
        throw badRequest("dayOfWeek must be Monday through Sunday or Everyday");
    }

    private static LocalTime parseTime(String value) {
        if (value == null || !LOCAL_MINUTES.matcher(value).matches()) {
            throw badRequest("startTime and endTime must use local HH:mm without dates, seconds or offsets");
        }
        return LocalTime.parse(value);
    }

    private static OnboardingAvailabilityException badRequest(String message) {
        return new OnboardingAvailabilityException(400, "BAD_REQUEST", message);
    }
}
