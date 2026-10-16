package org.sfa.volunteer.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.sfa.volunteer.service.impl.UserServiceImpl;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the actual profile-image auth/read/write code paths against an H2
 * (Postgres-compatibility-mode) schema that mirrors the real production
 * `users` table (per saayam-for-all/database wiki + the SQLGrammarExceptions
 * observed in CloudWatch while debugging issue #165), to catch column drift
 * before a slow Lambda-jar redeploy cycle. H2 can be more lenient than real
 * Postgres about type coercion, so this doesn't replace production
 * verification, but it catches wrong/missing column names reliably.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProfileImageSchemaIntegrationTest {

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:profileimagetest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        // We hand-create the schema below to match real column names/types;
        // don't let Hibernate derive it from the entity (that would hide the
        // exact drift we're trying to catch).
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, null, null, null, null, null, null);

        jdbcTemplate.execute("drop table if exists users");
        jdbcTemplate.execute("""
                create table users (
                    user_id varchar(64) primary key,
                    addr_ln1 varchar(255),
                    addr_ln2 varchar(255),
                    addr_ln3 varchar(255),
                    city_name varchar(255),
                    country_id integer,
                    first_name varchar(255),
                    full_name varchar(255),
                    gender varchar(50),
                    language_1 bigint,
                    language_2 bigint,
                    language_3 bigint,
                    last_location varchar(255),
                    last_name varchar(255),
                    last_updated_at timestamp,
                    middle_name varchar(255),
                    primary_email_address varchar(255),
                    primary_phone_number varchar(50),
                    profile_picture_path varchar(500),
                    state_id varchar(64),
                    time_zone varchar(100),
                    user_status_id bigint,
                    promotion_wizard_stage integer,
                    promotion_wizard_last_updated_at timestamp,
                    zip_code varchar(20)
                )
                """);

        jdbcTemplate.update(
                "insert into users (user_id, full_name, primary_email_address, time_zone, last_updated_at) "
                        + "values (?, ?, ?, ?, ?)",
                "SID-TEST-1", "Jane Doe", "Jane.Doe@Example.com", "America/New_York",
                Timestamp.from(ZonedDateTime.now(ZoneId.of("UTC")).toInstant()));
    }

    @Test
    void endToEndProfileImageFlowAgainstRealSchema() {
        // case-insensitive, trimmed auth check (issue #165 fix): does this
        // specific userId own this email?
        String userId = "SID-TEST-1";
        assertThat(userService.isEmailOwnedByUser(userId, " jane.doe@example.com ")).isTrue();
        assertThat(userService.isEmailOwnedByUser("some-other-user", " jane.doe@example.com ")).isFalse();

        // narrow write: must not attempt to write language_1/2/3 (bigint) or any
        // other column it doesn't need
        userService.setProfilePicturePath(userId, "s3://bucket/users/SID-TEST-1/profile");

        // narrow read
        assertThat(userService.getProfilePicturePath(userId))
                .contains("s3://bucket/users/SID-TEST-1/profile");
    }
}
