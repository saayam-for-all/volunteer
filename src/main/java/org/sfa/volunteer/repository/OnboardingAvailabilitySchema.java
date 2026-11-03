package org.sfa.volunteer.repository;

import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Checked per feature invocation rather than preventing application/Profile startup. */
@Component
public class OnboardingAvailabilitySchema {
    private static final Set<String> TEXT = Set.of("character varying", "text", "character");
    private static final Set<String> INTEGER = Set.of("integer", "smallint");
    private static final Set<String> TIMESTAMP = Set.of("timestamp without time zone");
    private static final Map<String, Set<String>> REQUIRED = Map.ofEntries(
            Map.entry("users.user_id", TEXT),
            Map.entry("users.time_zone", TEXT),
            Map.entry("users.primary_email_address", TEXT),
            Map.entry("users.promotion_wizard_stage", INTEGER),
            Map.entry("users.promotion_wizard_last_updated_at",
                    Set.of("timestamp without time zone", "timestamp with time zone")),
            Map.entry("users.is_emergency_available", Set.of("boolean")),
            Map.entry("volunteer_details.user_id", TEXT),
            Map.entry("volunteer_details.availability_days", Set.of("json", "jsonb")),
            Map.entry("volunteer_details.availability_times", Set.of("json", "jsonb")),
            Map.entry("user_availability.user_availability_id", INTEGER),
            Map.entry("user_availability.user_id", TEXT),
            Map.entry("user_availability.day_of_week", TEXT),
            Map.entry("user_availability.start_time", TIMESTAMP),
            Map.entry("user_availability.end_time", TIMESTAMP),
            Map.entry("user_availability.last_updated_at", TIMESTAMP));

    private final JdbcTemplate jdbc;

    public OnboardingAvailabilitySchema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void requireReady() {
        Map<String, String> types = new HashMap<>();
        Map<String, String> nullable = new HashMap<>();
        jdbc.query("""
                SELECT table_name, column_name, data_type, is_nullable FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name IN ('users', 'user_availability', 'volunteer_details')
                """, (org.springframework.jdbc.core.RowCallbackHandler) row -> {
            String key = row.getString("table_name") + "." + row.getString("column_name");
            types.put(key, row.getString("data_type"));
            nullable.put(key, row.getString("is_nullable"));
        });
        boolean ready = REQUIRED.entrySet().stream()
                .allMatch(field -> field.getValue().contains(types.getOrDefault(field.getKey(), "")))
                && "NO".equals(nullable.get("users.is_emergency_available"));
        if (!ready) {
            throw new OnboardingAvailabilityException(503, "FEATURE_UNAVAILABLE",
                    "Onboarding availability is not ready in this environment");
        }
    }
}
