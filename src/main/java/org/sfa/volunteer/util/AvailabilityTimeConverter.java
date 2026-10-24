package org.sfa.volunteer.util;

import org.sfa.volunteer.model.AvailabilityFrequency;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Converts weekly availability slots between the user's local time and the UTC values stored in
 * user_availability.
 *
 * <p>A weekly slot has no date, but a UTC offset depends on the date (daylight saving). Each slot
 * is therefore anchored on its next occurrence from {@code today}, and that real UTC instant is
 * stored. Converting it back uses the same instant, so the user reads back exactly what they saved.
 */
public final class AvailabilityTimeConverter {

    public record UtcSlot(DayOfWeek day, LocalDateTime start, LocalDateTime end) {}

    public record LocalSlot(DayOfWeek day, LocalTime start, LocalTime end) {}

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY;

    private AvailabilityTimeConverter() {
    }

    /** Local day + times in {@code zone} to UTC. An end at or before the start falls on the next day. */
    public static UtcSlot toUtc(DayOfWeek day, LocalTime start, LocalTime end, ZoneId zone, LocalDate today) {
        LocalDate anchor = today.with(TemporalAdjusters.nextOrSame(day));
        LocalDate endDate = end.isAfter(start) ? anchor : anchor.plusDays(1);

        LocalDateTime startUtc = toUtc(anchor.atTime(start).atZone(zone));
        LocalDateTime endUtc = toUtc(endDate.atTime(end).atZone(zone));
        return new UtcSlot(startUtc.getDayOfWeek(), startUtc, endUtc);
    }

    /** Stored UTC start/end back to the local day and times in {@code zone}. */
    public static LocalSlot toLocal(LocalDateTime startUtc, LocalDateTime endUtc, ZoneId zone) {
        ZonedDateTime start = startUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(zone);
        ZonedDateTime end = endUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(zone);
        return new LocalSlot(start.getDayOfWeek(), start.toLocalTime(), end.toLocalTime());
    }

    /**
     * Whether two local slots share any time in the week. Slots that only touch (10:00-14:00 and
     * 14:00-16:00) do not overlap. Overnight slots run into the next day, and Sunday night wraps into Monday.
     */
    public static boolean overlaps(LocalSlot a, LocalSlot b) {
        int aStart = minuteOfWeek(a.day(), a.start());
        int bStart = minuteOfWeek(b.day(), b.start());
        int aEnd = aStart + durationMinutes(a);
        int bEnd = bStart + durationMinutes(b);
        for (int shift : new int[]{-MINUTES_PER_WEEK, 0, MINUTES_PER_WEEK}) {
            if (aStart < bEnd + shift && bStart + shift < aEnd) {
                return true;
            }
        }
        return false;
    }

    /**
     * The frequencies that cover exactly {@code days}, in display order: all seven days become
     * Everyday; otherwise Weekdays and Weekends are taken out when complete and the rest stay single days.
     */
    public static List<AvailabilityFrequency> collapse(Set<DayOfWeek> days) {
        List<AvailabilityFrequency> result = new ArrayList<>();
        if (days.isEmpty()) {
            return result;
        }
        if (days.containsAll(AvailabilityFrequency.EVERYDAY.getDays())) {
            result.add(AvailabilityFrequency.EVERYDAY);
            return result;
        }

        Set<DayOfWeek> remaining = EnumSet.copyOf(days);
        for (AvailabilityFrequency group : List.of(AvailabilityFrequency.WEEKDAYS, AvailabilityFrequency.WEEKENDS)) {
            if (remaining.containsAll(group.getDays())) {
                result.add(group);
                remaining.removeAll(group.getDays());
            }
        }
        // EnumSet iterates Monday to Sunday
        remaining.forEach(day -> result.add(AvailabilityFrequency.of(day)));
        return result;
    }

    private static LocalDateTime toUtc(ZonedDateTime local) {
        return local.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static int minuteOfWeek(DayOfWeek day, LocalTime time) {
        return (day.getValue() - 1) * MINUTES_PER_DAY + time.getHour() * 60 + time.getMinute();
    }

    private static int durationMinutes(LocalSlot slot) {
        int minutes = (int) Duration.between(slot.start(), slot.end()).toMinutes();
        return minutes > 0 ? minutes : minutes + MINUTES_PER_DAY;
    }
}
