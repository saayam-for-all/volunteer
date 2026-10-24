package org.sfa.volunteer.util;

import org.junit.jupiter.api.Test;
import org.sfa.volunteer.model.AvailabilityFrequency;
import org.sfa.volunteer.util.AvailabilityTimeConverter.LocalSlot;
import org.sfa.volunteer.util.AvailabilityTimeConverter.UtcSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;

import static java.time.DayOfWeek.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sfa.volunteer.model.AvailabilityFrequency.EVERYDAY;
import static org.sfa.volunteer.model.AvailabilityFrequency.WEEKDAYS;
import static org.sfa.volunteer.model.AvailabilityFrequency.WEEKENDS;

class AvailabilityTimeConverterTest {

    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    // Mondays: one in daylight saving time (UTC-5 in Chicago), one in standard time (UTC-6)
    private static final LocalDate SUMMER = LocalDate.of(2026, 7, 6);
    private static final LocalDate WINTER = LocalDate.of(2026, 1, 5);

    private static LocalTime t(String hhmm) {
        return LocalTime.parse(hhmm);
    }

    @Test
    void lateEveningMovesToNextUtcDay() {
        UtcSlot utc = AvailabilityTimeConverter.toUtc(MONDAY, t("22:00"), t("23:00"), CHICAGO, SUMMER);

        assertThat(utc.day()).isEqualTo(TUESDAY);
        assertThat(utc.start()).isEqualTo(LocalDateTime.of(2026, 7, 7, 3, 0));
        assertThat(utc.end()).isEqualTo(LocalDateTime.of(2026, 7, 7, 4, 0));
    }

    @Test
    void earlyMorningEastOfUtcMovesToPreviousUtcDay() {
        UtcSlot utc = AvailabilityTimeConverter.toUtc(MONDAY, t("02:00"), t("04:00"), KOLKATA, SUMMER);

        assertThat(utc.day()).isEqualTo(SUNDAY);
        assertThat(utc.start()).isEqualTo(LocalDateTime.of(2026, 7, 5, 20, 30));
    }

    @Test
    void overnightEndFallsOnNextDay() {
        UtcSlot utc = AvailabilityTimeConverter.toUtc(SATURDAY, t("22:00"), t("02:00"), CHICAGO, SUMMER);

        assertThat(utc.day()).isEqualTo(SUNDAY);
        assertThat(utc.start()).isEqualTo(LocalDateTime.of(2026, 7, 12, 3, 0));
        assertThat(utc.end()).isEqualTo(LocalDateTime.of(2026, 7, 12, 7, 0));
    }

    @Test
    void anchorsOnNextOccurrenceSoOffsetFollowsDaylightSaving() {
        UtcSlot summer = AvailabilityTimeConverter.toUtc(MONDAY, t("09:00"), t("17:00"), CHICAGO, SUMMER);
        UtcSlot winter = AvailabilityTimeConverter.toUtc(MONDAY, t("09:00"), t("17:00"), CHICAGO, WINTER);

        assertThat(summer.start().toLocalTime()).isEqualTo(t("14:00"));
        assertThat(winter.start().toLocalTime()).isEqualTo(t("15:00"));
    }

    @Test
    void savingJustBeforeDstEndsAnchorsEachDayOnItsOwnOffset() {
        // Saturday 2026-10-31 is still CDT (UTC-5); daylight saving ends on Sunday 2026-11-01
        LocalDate dayBeforeDstEnds = LocalDate.of(2026, 10, 31);

        UtcSlot saturday = AvailabilityTimeConverter.toUtc(SATURDAY, t("09:00"), t("10:00"), CHICAGO, dayBeforeDstEnds);
        UtcSlot monday = AvailabilityTimeConverter.toUtc(MONDAY, t("09:00"), t("10:00"), CHICAGO, dayBeforeDstEnds);

        assertThat(saturday.start()).isEqualTo(LocalDateTime.of(2026, 10, 31, 14, 0));
        // next Monday is 2026-11-02, already CST (UTC-6)
        assertThat(monday.start()).isEqualTo(LocalDateTime.of(2026, 11, 2, 15, 0));
        // both read back as the 09:00 the user saved
        assertThat(AvailabilityTimeConverter.toLocal(saturday.start(), saturday.end(), CHICAGO))
                .isEqualTo(new LocalSlot(SATURDAY, t("09:00"), t("10:00")));
        assertThat(AvailabilityTimeConverter.toLocal(monday.start(), monday.end(), CHICAGO))
                .isEqualTo(new LocalSlot(MONDAY, t("09:00"), t("10:00")));
    }

    @Test
    void overlapsOnTheSameDay() {
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(MONDAY, t("10:00"), t("14:00")),
                new LocalSlot(MONDAY, t("12:00"), t("16:00")))).isTrue();
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(MONDAY, t("09:00"), t("17:00")),
                new LocalSlot(MONDAY, t("12:00"), t("13:00")))).isTrue();
    }

    @Test
    void separateAndTouchingSlotsDoNotOverlap() {
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(MONDAY, t("10:00"), t("14:00")),
                new LocalSlot(MONDAY, t("16:00"), t("18:00")))).isFalse();
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(MONDAY, t("10:00"), t("14:00")),
                new LocalSlot(MONDAY, t("14:00"), t("16:00")))).isFalse();
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(MONDAY, t("10:00"), t("14:00")),
                new LocalSlot(TUESDAY, t("10:00"), t("14:00")))).isFalse();
    }

    @Test
    void overnightSlotOverlapsTheNextMorning() {
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(MONDAY, t("22:00"), t("02:00")),
                new LocalSlot(TUESDAY, t("01:00"), t("03:00")))).isTrue();
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(MONDAY, t("22:00"), t("02:00")),
                new LocalSlot(TUESDAY, t("02:00"), t("03:00")))).isFalse();
    }

    @Test
    void sundayOvernightWrapsIntoMonday() {
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(SUNDAY, t("23:00"), t("01:00")),
                new LocalSlot(MONDAY, t("00:30"), t("02:00")))).isTrue();
        assertThat(AvailabilityTimeConverter.overlaps(
                new LocalSlot(MONDAY, t("00:30"), t("02:00")),
                new LocalSlot(SUNDAY, t("23:00"), t("01:00")))).isTrue();
    }

    @Test
    void laterDayInWeekAnchorsOnUpcomingDate() {
        UtcSlot utc = AvailabilityTimeConverter.toUtc(FRIDAY, t("09:00"), t("10:00"), CHICAGO, SUMMER);

        assertThat(utc.start().toLocalDate()).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    void roundTripsBackToTheSavedLocalSlot() {
        for (ZoneId zone : new ZoneId[]{CHICAGO, KOLKATA, ZoneId.of("UTC")}) {
            for (LocalDate today : new LocalDate[]{SUMMER, WINTER}) {
                for (DayOfWeek day : DayOfWeek.values()) {
                    UtcSlot utc = AvailabilityTimeConverter.toUtc(day, t("22:30"), t("01:15"), zone, today);

                    LocalSlot local = AvailabilityTimeConverter.toLocal(utc.start(), utc.end(), zone);

                    assertThat(local).isEqualTo(new LocalSlot(day, t("22:30"), t("01:15")));
                }
            }
        }
    }

    @Test
    void collapsesAllSevenDaysToEveryday() {
        assertThat(AvailabilityTimeConverter.collapse(EnumSet.allOf(DayOfWeek.class)))
                .containsExactly(EVERYDAY);
    }

    @Test
    void collapsesWeekdaysAndWeekends() {
        assertThat(AvailabilityTimeConverter.collapse(EnumSet.range(MONDAY, FRIDAY))).containsExactly(WEEKDAYS);
        assertThat(AvailabilityTimeConverter.collapse(EnumSet.of(SATURDAY, SUNDAY))).containsExactly(WEEKENDS);
    }

    @Test
    void keepsLeftoverDaysAsSingleDays() {
        assertThat(AvailabilityTimeConverter.collapse(EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)))
                .containsExactly(WEEKDAYS, AvailabilityFrequency.SATURDAY);
        assertThat(AvailabilityTimeConverter.collapse(EnumSet.of(SUNDAY, MONDAY, SATURDAY)))
                .containsExactly(WEEKENDS, AvailabilityFrequency.MONDAY);
        assertThat(AvailabilityTimeConverter.collapse(EnumSet.of(WEDNESDAY, MONDAY)))
                .containsExactly(AvailabilityFrequency.MONDAY, AvailabilityFrequency.WEDNESDAY);
    }

    @Test
    void collapsesEmptySetToNothing() {
        assertThat(AvailabilityTimeConverter.collapse(EnumSet.noneOf(DayOfWeek.class))).isEmpty();
    }
}
