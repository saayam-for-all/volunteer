package org.sfa.volunteer.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 4);

    @Test
    void returnsValid_whenExpiryIsWellInTheFuture() {
        assertEquals(DocumentStatus.VALID,
                DocumentStatus.from(TODAY.plusDays(90), TODAY));
    }

    @Test
    void returnsValid_whenExpiryIsJustOutsideThreshold() {
        assertEquals(DocumentStatus.VALID,
                DocumentStatus.from(TODAY.plusDays(31), TODAY));
    }

    @Test
    void returnsExpiringSoon_atThresholdBoundary() {
        assertEquals(DocumentStatus.EXPIRING_SOON,
                DocumentStatus.from(TODAY.plusDays(30), TODAY));
    }

    @Test
    void returnsExpiringSoon_whenExpiryIsNear() {
        assertEquals(DocumentStatus.EXPIRING_SOON,
                DocumentStatus.from(TODAY.plusDays(1), TODAY));
    }

    @Test
    void returnsExpiringSoon_whenExpiryIsToday() {
        assertEquals(DocumentStatus.EXPIRING_SOON,
                DocumentStatus.from(TODAY, TODAY));
    }

    @Test
    void returnsExpired_whenExpiryWasYesterday() {
        assertEquals(DocumentStatus.EXPIRED,
                DocumentStatus.from(TODAY.minusDays(1), TODAY));
    }

    @Test
    void returnsExpired_whenExpiryIsLongPast() {
        assertEquals(DocumentStatus.EXPIRED,
                DocumentStatus.from(TODAY.minusDays(400), TODAY));
    }

    @Test
    void rejectsNullExpiry() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentStatus.from(null, TODAY));
    }

    @Test
    void rejectsNullToday() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentStatus.from(TODAY, null));
    }
}