package org.sfa.volunteer.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Lifecycle state of a stored identity document, derived from its expiry date.
 *
 * A document that has already expired is still shown to the volunteer rather
 * than hidden - they need to see what they are replacing - so EXPIRED is a
 * display state, not an error.
 *
 * Absence of a document is deliberately not modelled here. The view handler
 * signals that with a 204, so this enum only describes documents that exist.
 */
public enum DocumentStatus {

    VALID,
    EXPIRING_SOON,
    EXPIRED;

    /** Days before expiry at which a document is considered expiring soon. */
    public static final int EXPIRING_SOON_THRESHOLD_DAYS = 30;

    /**
     * @param expiresOn the document's expiry date, must not be null
     * @param today     the date to evaluate against, passed in rather than read
     *                  from the clock so the calculation stays testable
     */
    public static DocumentStatus from(LocalDate expiresOn, LocalDate today) {
        if (expiresOn == null) {
            throw new IllegalArgumentException("expiresOn is required");
        }
        if (today == null) {
            throw new IllegalArgumentException("today is required");
        }

        if (expiresOn.isBefore(today)) {
            return EXPIRED;
        }

        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiresOn);
        if (daysUntilExpiry <= EXPIRING_SOON_THRESHOLD_DAYS) {
            return EXPIRING_SOON;
        }

        return VALID;
    }

    /** Convenience overload for callers that do not need a fixed clock. */
    public static DocumentStatus from(LocalDate expiresOn) {
        return from(expiresOn, LocalDate.now());
    }
}