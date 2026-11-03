package org.sfa.volunteer.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Narrow account access shared by Profile and onboarding; never selects the emergency column. */
@Repository
public class AvailabilityAccountRepository {
    public record Account(String userId, String timeZone, String email, Integer promotionWizardStage) {}

    private static final String ACCOUNT = """
            SELECT user_id, time_zone, primary_email_address, promotion_wizard_stage
            FROM users WHERE user_id = ?
            """;
    private final JdbcTemplate jdbc;

    public AvailabilityAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Account> findById(String userId) {
        return query(ACCOUNT, userId);
    }

    public Map<String, Account> findByIds(Collection<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        return jdbc.query("""
                SELECT user_id, time_zone, primary_email_address, promotion_wizard_stage
                FROM users WHERE user_id IN (
                """ + placeholders + ")", AvailabilityAccountRepository::account, userIds.toArray())
                .stream().collect(Collectors.toMap(Account::userId, value -> value));
    }

    /** The outer application transaction holds this lock until slots and account state commit. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Account> lockById(String userId) {
        return query(ACCOUNT + " FOR UPDATE", userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateTimezone(String userId, String timezone) {
        jdbc.update("UPDATE users SET time_zone = ? WHERE user_id = ?", timezone, userId);
    }

    private Optional<Account> query(String sql, String userId) {
        return jdbc.query(sql, AvailabilityAccountRepository::account, userId).stream().findFirst();
    }

    private static Account account(ResultSet row, int index) throws SQLException {
        return new Account(row.getString("user_id"),
                row.getString("time_zone"), row.getString("primary_email_address"),
                row.getObject("promotion_wizard_stage", Integer.class));
    }
}
