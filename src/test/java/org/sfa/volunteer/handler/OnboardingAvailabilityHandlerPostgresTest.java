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
    static void bootLambda() {
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
    void lambdaAdvertisesAllowedMethodsAndRejectsUnsupportedWrites() throws Exception {
        var preflight = handler.handleRequest(new APIGatewayProxyRequestEvent().withHttpMethod("OPTIONS"), null);
        assertThat(preflight.getStatusCode()).isEqualTo(204);
        assertThat(preflight.getBody()).isNull();
        assertThat(preflight.getHeaders().get("Access-Control-Allow-Methods")).isEqualTo("POST,PUT,OPTIONS");

        var denied = handler.handleRequest(event("DELETE", SAVE, verifiedClaims(OWNER)), null);
        assertThat(denied.getHeaders().get("Allow")).isEqualTo("POST,PUT,OPTIONS");
        assertFailure(denied, 405, "METHOD_NOT_ALLOWED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
    }

    @Test
    void base64GatewayBodiesHaveTheSameContractAndInvalidEncodingCannotWrite() throws Exception {
        var save = event("PUT", Base64.getEncoder().encodeToString(SAVE.getBytes(UTF_8)), verifiedClaims(OWNER))
                .withIsBase64Encoded(true);
        var saved = handler.handleRequest(save, null);
        assertThat(saved.getStatusCode()).isEqualTo(200);

        var badEncoding = event("PUT", "%invalid%", verifiedClaims(OWNER)).withIsBase64Encoded(true);
        assertFailure(handler.handleRequest(badEncoding, null), 400, "BAD_REQUEST");

        var read = event("POST", Base64.getEncoder().encodeToString(READ.getBytes(UTF_8)), verifiedClaims(OWNER))
                .withIsBase64Encoded(true);
        var loaded = handler.handleRequest(read, null);
        assertThat(loaded.getStatusCode()).isEqualTo(200);
        assertThat(body(loaded).path("data")).isEqualTo(body(saved).path("data"));
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

    @Test
    void bothLambdaWritersRequireVerifiedOwnerClaimsAndPreserveTheSavedSchedule() throws Exception {
        var saved = handler.handleRequest(event("PUT", SAVE, verifiedClaims(OWNER)), null);
        assertThat(saved.getStatusCode()).isEqualTo(200);
        String changed = SAVE.replace("09:00", "10:00");
        var forged = new APIGatewayProxyRequestEvent().withHttpMethod("PUT")
                .withHeaders(Map.of("X-Caller-Email", OWNER, "Authorization", "Bearer synthetic"))
                .withBody(changed.replace("\"step\":4", "\"step\":4,\"email\":\"" + OWNER + "\""));

        for (var route : List.of(handler, generic)) {
            assertFailure(route.handleRequest(forged, null), 403, "FORBIDDEN");
            assertFailure(route.handleRequest(event("PUT", changed, verifiedClaims("other@example.test")), null),
                    403, "FORBIDDEN");
            assertFailure(route.handleRequest(event("PUT", changed,
                    Map.of("sub", "synthetic-subject", "email", OWNER, "email_verified", "false")), null),
                    403, "FORBIDDEN");
            assertFailure(route.handleRequest(event("PUT", changed,
                    Map.of("email", OWNER, "email_verified", true)), null), 403, "FORBIDDEN");
        }
        assertFailure(handler.handleRequest(event("POST", READ, verifiedClaims("other@example.test")), null),
                403, "FORBIDDEN");
        var loaded = handler.handleRequest(event("POST", READ, verifiedClaims(OWNER)), null);
        assertThat(body(loaded).path("data")).isEqualTo(body(saved).path("data"));
    }

    @Test
    void bothLambdaWritersRejectLegacyStepFourPayloadsAndCoercibleValues() throws Exception {
        for (String invalid : List.of(SAVE.replace("09:00", "2026-11-02T09:00:00Z"),
                SAVE.replace("true", "\"true\""), SAVE.replace("\"step\":4", "\"step\":\"4\""),
                SAVE.replace("\"step\":4", "\"step\":4.0"),
                SAVE.replace("\"isEmergencyAvailable\":true,", ""), SAVE + "{}", "{")) {
            var request = event("PUT", invalid, verifiedClaims(OWNER));
            assertFailure(handler.handleRequest(request, null), 400, "BAD_REQUEST");
            assertFailure(generic.handleRequest(request, null), 400, "BAD_REQUEST");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT availability_times::text FROM volunteer_details", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT promotion_wizard_stage FROM users", Integer.class)).isEqualTo(3);
    }

    @Test
    void genericStepFourOnlyWritesThroughPutAndAcceptsBase64() throws Exception {
        var preflight = generic.handleRequest(new APIGatewayProxyRequestEvent().withHttpMethod("OPTIONS"), null);
        assertThat(preflight.getStatusCode()).isEqualTo(204);
        assertThat(preflight.getHeaders().get("Access-Control-Allow-Methods")).isEqualTo("PUT,OPTIONS");
        var rejected = generic.handleRequest(event("POST", SAVE, verifiedClaims(OWNER)), null);
        assertFailure(rejected, 405, "METHOD_NOT_ALLOWED");
        assertThat(rejected.getHeaders().get("Allow")).isEqualTo("PUT,OPTIONS");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
        var encoded = event("PUT", Base64.getEncoder().encodeToString(SAVE.getBytes(UTF_8)), verifiedClaims(OWNER))
                .withIsBase64Encoded(true);
        assertThat(generic.handleRequest(encoded, null).getStatusCode()).isEqualTo(200);
    }

    @Test
    void lambdaReturnsActualStatusesForMissingAccountTimezoneAndSchema() throws Exception {
        assertFailure(handler.handleRequest(event("POST", READ.replace(USER_ID, "SID-MISSING"),
                verifiedClaims(OWNER)), null), 404, "USER_NOT_FOUND");
        jdbc.update("UPDATE users SET time_zone = 'not-an-iana-zone' WHERE user_id = ?", USER_ID);
        assertFailure(handler.handleRequest(event("POST", READ, verifiedClaims(OWNER)), null),
                409, "ACCOUNT_TIMEZONE_REQUIRED");
        jdbc.execute("ALTER TABLE users DROP COLUMN is_emergency_available");
        assertFailure(generic.handleRequest(event("PUT", SAVE, verifiedClaims(OWNER)), null),
                503, "FEATURE_UNAVAILABLE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
    }

    @Test
    void missingWizardStageColumnReturnsUnavailableWithoutSaving() throws Exception {
        jdbc.execute("ALTER TABLE users DROP COLUMN promotion_wizard_stage");
        assertFailure(handler.handleRequest(event("PUT", SAVE, Map.of()), null), 403, "FORBIDDEN");
        assertFailure(handler.handleRequest(event("PUT", SAVE, verifiedClaims(OWNER)), null),
                503, "FEATURE_UNAVAILABLE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
    }

    @Test
    void storageFailureReturnsSanitized500AndRollsBackReplacement() throws Exception {
        var saved = handler.handleRequest(event("PUT", SAVE, verifiedClaims(OWNER)), null);
        assertThat(saved.getStatusCode()).isEqualTo(200);
        jdbc.execute("ALTER TABLE user_availability ADD CONSTRAINT synthetic_write_failure CHECK (day_of_week <> 'Tuesday')");
        var failed = generic.handleRequest(event("PUT", SAVE.replace("Monday", "Tuesday").replace("true", "false"),
                verifiedClaims(OWNER)), null);
        assertFailure(failed, 500, "INTERNAL_SERVER_ERROR");
        assertThat(failed.getBody()).doesNotContain("synthetic_write_failure", "user_availability", "INSERT");
        var loaded = handler.handleRequest(event("POST", READ, verifiedClaims(OWNER)), null);
        assertThat(body(loaded).path("data")).isEqualTo(body(saved).path("data"));
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
