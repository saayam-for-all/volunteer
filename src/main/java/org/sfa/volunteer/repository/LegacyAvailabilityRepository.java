package org.sfa.volunteer.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reads and clears the legacy JSON without hydrating Volunteer or User entities. */
@Repository
public class LegacyAvailabilityRepository {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final JdbcTemplate jdbc;

    public LegacyAvailabilityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean containsSchedule(String userId) {
        return jdbc.query("""
                SELECT availability_days, availability_times FROM volunteer_details WHERE user_id = ?
                """, (row, index) -> hasContent(parse(row.getString(1))) || hasContent(parse(row.getString(2))), userId)
                .stream().anyMatch(Boolean::booleanValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void clear(String userId) {
        jdbc.update("""
                UPDATE volunteer_details SET availability_days = NULL, availability_times = NULL
                WHERE user_id = ? AND (availability_days IS NOT NULL OR availability_times IS NOT NULL)
                """, userId);
    }

    public static boolean hasContent(JsonNode value) {
        if (value == null || value.isNull()) return false;
        if (value.isContainerNode()) return !value.isEmpty();
        return !value.isTextual() || !value.textValue().isBlank();
    }

    private static JsonNode parse(String value) {
        if (value == null) return null;
        try {
            return JSON.readTree(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Unable to read legacy availability data");
        }
    }
}
