package org.sfa.volunteer.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** State needed only by onboarding; Profile never selects the emergency column. */
@Repository
public class OnboardingAvailabilityRepository {
    private final JdbcTemplate jdbc;

    public OnboardingAvailabilityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean volunteerExists(String userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM volunteer_details WHERE user_id = ?)", Boolean.class, userId));
    }

    public boolean emergencyAvailable(String userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT is_emergency_available FROM users WHERE user_id = ?", Boolean.class, userId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveState(String userId, boolean emergency, LocalDateTime updatedAt) {
        jdbc.update("""
                UPDATE users SET is_emergency_available = ?,
                    promotion_wizard_stage = GREATEST(COALESCE(promotion_wizard_stage, 0), 4),
                    promotion_wizard_last_updated_at =
                """ + progressTimestampSql() + " WHERE user_id = ?", emergency, updatedAt.atOffset(ZoneOffset.UTC), userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void advanceProgress(String userId, int step, LocalDateTime updatedAt) {
        jdbc.update("""
                UPDATE users SET promotion_wizard_stage = GREATEST(COALESCE(promotion_wizard_stage, 0), ?),
                    promotion_wizard_last_updated_at =
                """ + progressTimestampSql() + " WHERE user_id = ? AND COALESCE(promotion_wizard_stage, 0) < ?",
                step, updatedAt.atOffset(ZoneOffset.UTC), userId, step);
    }

    private String progressTimestampSql() {
        String timestampType = jdbc.queryForObject("""
                SELECT data_type FROM information_schema.columns WHERE table_schema=current_schema()
                  AND table_name='users' AND column_name='promotion_wizard_last_updated_at'
                """, String.class);
        // Preserve the UTC instant for either existing progress-column layout, independently
        // of the PostgreSQL session timezone. SQL fragments are fixed, never request values.
        return "timestamp with time zone".equals(timestampType)
                ? "?::timestamptz" : "(?::timestamptz AT TIME ZONE 'UTC')";
    }
}
