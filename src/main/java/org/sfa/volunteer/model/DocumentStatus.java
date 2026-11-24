package org.sfa.volunteer.model;

import java.time.LocalDate;

public enum DocumentStatus {

    VALID,
    EXPIRING_SOON,
    EXPIRED;

    /** Months before expiry at which a document is considered expiring soon. */
    public static final int EXPIRING_SOON_THRESHOLD_MONTHS = 3;

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

        if (!expiresOn.isAfter(today.plusMonths(EXPIRING_SOON_THRESHOLD_MONTHS))) {
            return EXPIRING_SOON;
        }

        return VALID;
    }

    /** Convenience overload for callers that do not need a fixed clock. */
    public static DocumentStatus from(LocalDate expiresOn) {
        return from(expiresOn, LocalDate.now());
    }
}