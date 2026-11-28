package org.sfa.volunteer.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sfa.volunteer.support.PostgresTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Executes the actual operator SQL, only against a validated dedicated local test database. */
@EnabledIfSystemProperty(named = "ba33.pg.enabled", matches = "true")
class OnboardingAvailabilityMigrationPostgresTest {
    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void fixture() {
        Map<String, Supplier<?>> properties = new HashMap<>();
        PostgresTestDatabase.configure(properties::put, "ba33_t2_migration");
        dataSource = new DriverManagerDataSource((String) properties.get("spring.datasource.url").get(),
                (String) properties.get("spring.datasource.username").get(),
                (String) properties.get("spring.datasource.password").get());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS ba33_t2_migration");
        jdbc.execute("DROP TABLE IF EXISTS users");
        jdbc.execute("CREATE TABLE users(user_id varchar(64) PRIMARY KEY)");
        jdbc.update("INSERT INTO users VALUES ('SID-MIGRATION')");
    }

    @Test
    void addsTheMissingFlagWithFalseBackfillDefaultAndNotNullConstraint() throws Exception {
        runMigration();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema()
                  AND table_name='users' AND column_name='is_emergency_available'
                  AND data_type='boolean' AND is_nullable='NO' AND column_default='false'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT is_emergency_available FROM users WHERE user_id='SID-MIGRATION'",
                Boolean.class)).isFalse();
        jdbc.update("INSERT INTO users(user_id) VALUES ('SID-DEFAULT')");
        assertThat(jdbc.queryForObject("SELECT is_emergency_available FROM users WHERE user_id='SID-DEFAULT'",
                Boolean.class)).isFalse();
    }

    @Test
    void preservesExistingTrueBackfillsNullAndCanBeRunAgain() throws Exception {
        jdbc.execute("ALTER TABLE users ADD COLUMN is_emergency_available boolean");
        jdbc.update("UPDATE users SET is_emergency_available=true WHERE user_id='SID-MIGRATION'");
        jdbc.update("INSERT INTO users(user_id) VALUES ('SID-NULL')");
        runMigration();
        runMigration();
        assertThat(jdbc.queryForObject("SELECT is_emergency_available FROM users WHERE user_id='SID-MIGRATION'",
                Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT is_emergency_available FROM users WHERE user_id='SID-NULL'",
                Boolean.class)).isFalse();
    }

    @Test
    void rejectsAnExistingNonBooleanColumnWithoutChangingIt() {
        jdbc.execute("ALTER TABLE users ADD COLUMN is_emergency_available varchar(20)");
        jdbc.update("UPDATE users SET is_emergency_available='legacy'");
        assertThatThrownBy(this::runMigration).isInstanceOf(SQLException.class).hasMessageContaining("boolean type");
        assertThat(jdbc.queryForObject("SELECT is_emergency_available FROM users", String.class)).isEqualTo("legacy");
        assertThat(jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns WHERE table_schema=current_schema()
                  AND table_name='users' AND column_name='is_emergency_available'
                """, String.class)).isEqualTo("YES");
    }

    @Test
    void aBackfillFailureRollsBackTheAddedColumnAndKeepsTheOriginalRows() {
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION reject_backfill() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'synthetic migration failure'; END $$
                """);
        jdbc.execute("CREATE TRIGGER reject_backfill BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION reject_backfill()");
        assertThatThrownBy(this::runMigration).isInstanceOf(SQLException.class)
                .hasMessageContaining("synthetic migration failure");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema()
                  AND table_name='users' AND column_name='is_emergency_available'
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    void refusesToCreateAUsersTableInAnUnpreparedSchema() {
        jdbc.execute("DROP TABLE users");
        assertThatThrownBy(this::runMigration).isInstanceOf(SQLException.class)
                .hasMessageContaining("selected schema must contain users");
    }

    private void runMigration() throws Exception {
        String sql = Files.readString(Path.of("db/migrations/20261030_ba33_emergency_availability.sql"));
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            try {
                statement.execute(sql);
            } catch (SQLException failure) {
                statement.execute("ROLLBACK");
                throw failure;
            }
        }
    }
}
