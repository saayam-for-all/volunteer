package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sfa.volunteer.support.PostgresTestDatabase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "ba33.pg.enabled", matches = "true")
class OnboardingAvailabilityHandlerPostgresTest {
    private static final String USER_ID = "SID-T5";
    private static final String OWNER = "owner-t5@example.test";
    private static final String READ = "{\"userId\":\"SID-T5\"}";
    private static final String SAVE = """
            {"userId":"SID-T5","step":4,"isEmergencyAvailable":true,
              "availability":[{"dayOfWeek":"Monday","startTime":"09:00","endTime":"12:00"}]}
            """;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static OnboardingAvailabilityHandler handler;
    private static UpdateVolunteerHandler generic;

    @BeforeAll
    static void bootLambda() throws Exception {
        var properties = new HashMap<>(PostgresTestDatabase.properties("ba33_t5"));
        dataSource = new DriverManagerDataSource(properties.get("spring.datasource.url"),
                properties.get("spring.datasource.username"), properties.get("spring.datasource.password"));
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS ba33_t5");
        properties.put("spring.config.location", "optional:classpath:/ba33-no-runtime-config/");
        properties.put("cors.allowed-origin", "https://example.test");
        properties.put("cors.allowed-headers", "Content-Type,Authorization");
        properties.put("cors.allowed-methods", "GET,POST,PUT,OPTIONS");
        properties.put("cors.credentials", "false");
        properties.put("saayam.s3.buckets.euPrivate", "ba33-test-unused-eu");
        properties.put("saayam.s3.buckets.usPrivate", "ba33-test-unused-us");
        var previous = new HashMap<String, String>();
        properties.forEach((key, value) -> previous.put(key, System.setProperty(key, value)));
        try {
            handler = new OnboardingAvailabilityHandler();
            generic = new UpdateVolunteerHandler();
        } finally {
            previous.forEach((key, value) -> {
                if (value == null) System.clearProperty(key);
                else System.setProperty(key, value);
            });
        }
    }

    @BeforeEach
    void fixture() {
        jdbc.execute("DROP TABLE IF EXISTS user_availability");
        jdbc.execute("DROP TABLE IF EXISTS volunteer_details");
        jdbc.execute("DROP TABLE IF EXISTS user_additional_details");
        jdbc.execute("DROP TABLE IF EXISTS users");
        jdbc.execute("DROP TABLE IF EXISTS user_status");
        new ResourceDatabasePopulator(new ClassPathResource("availability214-schema.sql")).execute(dataSource);
        jdbc.execute("""
                ALTER TABLE users ALTER COLUMN language_1 TYPE varchar(50),
                    ALTER COLUMN language_2 TYPE varchar(50), ALTER COLUMN language_3 TYPE varchar(50)
                """);
        jdbc.execute("ALTER TABLE users ADD COLUMN is_emergency_available boolean NOT NULL DEFAULT false");
        jdbc.execute("""
                CREATE TABLE user_status (user_status_id bigint PRIMARY KEY, user_status varchar(50),
                    user_status_desc varchar(100), last_update_date timestamp)
                """);
        jdbc.execute("INSERT INTO user_status(user_status_id, user_status) VALUES (1, 'Active')");
        jdbc.execute("""
                CREATE TABLE volunteer_details (
                    user_id varchar(255) PRIMARY KEY REFERENCES users(user_id),
                    availability_days jsonb, availability_times jsonb,
                    terms_and_conditions boolean, terms_accepted_at timestamp,
                    govt_id_path1 varchar(255), govt_id_path2 varchar(255),
                    govt_id_name1 varchar(255), govt_id_name2 varchar(255),
                    govt_id_expiry1 date, govt_id_expiry2 date,
                    path1_updated_at timestamp, path2_updated_at timestamp,
                    created_at timestamp, last_updated_at timestamp
                )
                """);
        jdbc.update("""
                INSERT INTO users(user_id, full_name, primary_email_address, time_zone,
                    promotion_wizard_stage, user_status_id)
                VALUES (?, 'Synthetic owner', ?, 'Asia/Kolkata', 3, 1)
                """, USER_ID, OWNER);
        jdbc.update("INSERT INTO volunteer_details(user_id) VALUES (?)", USER_ID);
    }

    @Test
    void verifiedOwnerSavesAndReadsTheSameScheduleThroughPublicLambda() throws Exception {
        var saved = handler.handleRequest(event("PUT", SAVE, verifiedClaims(OWNER)), null);
        assertThat(saved.getStatusCode()).isEqualTo(200);
        var envelope = body(saved);
        assertThat(envelope.path("success").asBoolean()).isTrue();
        assertThat(envelope.path("statusCode").asInt()).isEqualTo(200);
        var schedule = envelope.path("data");
        assertThat(schedule.path("userId").asText()).isEqualTo(USER_ID);
        assertThat(schedule.path("isEmergencyAvailable").asBoolean()).isTrue();
        assertThat(schedule.path("promotionWizardStage").asInt()).isEqualTo(4);
        assertThat(schedule.at("/availability/0/dayOfWeek").asText()).isEqualTo("Monday");
        assertThat(schedule.at("/availability/0/startTime").asText()).isEqualTo("09:00");
        assertThat(schedule.at("/availability/0/endTime").asText()).isEqualTo("12:00");
        var read = handler.handleRequest(event("POST", READ, verifiedClaims(OWNER)), null);
        assertThat(read.getStatusCode()).isEqualTo(200);
        assertThat(body(read).path("data")).isEqualTo(schedule);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isEqualTo(1);
    }

    @Test
    void genericUpdateKeepsEarlierStepsAndSavesStepFourBeforeLegacyBinding() throws Exception {
        var terms = generic.handleRequest(event("PUT", """
                {"userId":"SID-T5","step":1,"termsAndConditions":true}
                """, Map.of()), null);
        assertThat(terms.getStatusCode()).isEqualTo(201);
        assertThat(body(terms).at("/data/termsAndConditions").asBoolean()).isTrue();

        var saved = generic.handleRequest(event("PUT", SAVE, verifiedClaims(OWNER)), null);
        assertThat(saved.getStatusCode()).isEqualTo(200);
        var loaded = handler.handleRequest(event("POST", READ, verifiedClaims(OWNER)), null);
        assertThat(loaded.getStatusCode()).isEqualTo(200);
        assertThat(body(loaded).path("data")).isEqualTo(body(saved).path("data"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT availability_times::text FROM volunteer_details", String.class)).isNull();
    }

    private static Map<String, String> verifiedClaims(String email) {
        return Map.of("sub", "synthetic-subject", "email", email, "email_verified", "true");
    }

    private static APIGatewayProxyRequestEvent event(String method, String body, Map<String, ?> claims) {
        var context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(Map.of("claims", claims));
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withRequestContext(context).withBody(body);
    }

    private static JsonNode body(APIGatewayProxyResponseEvent response) throws Exception {
        return JSON.readTree(response.getBody());
    }

    private static void assertFailure(APIGatewayProxyResponseEvent response, int status, String code) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(status);
        var envelope = body(response);
        assertThat(envelope.path("success").asBoolean()).isFalse();
        assertThat(envelope.path("statusCode").asInt()).isEqualTo(status);
        assertThat(envelope.path("saayamCode").asText()).isEqualTo(code);
        assertThat(envelope.has("data")).isFalse();
    }
}
