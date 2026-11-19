package org.sfa.volunteer.dto.common;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;

/**
 * The values of the PostgreSQL {@code preference_type} enum backing
 * {@code user_notification_preferences.preference}.
 *
 * <p>The database spells these in lower case, so {@link #getValue()} is what gets
 * cast into the column; {@link #fromValue(String)} accepts either case from callers.
 */
@Getter
public enum NotificationPreferenceType {

    EMAIL("email"),
    TEXT("text"),
    BOTH("both");

    private final String value;

    NotificationPreferenceType(String value) {
        this.value = value;
    }

    public static Optional<NotificationPreferenceType> fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
