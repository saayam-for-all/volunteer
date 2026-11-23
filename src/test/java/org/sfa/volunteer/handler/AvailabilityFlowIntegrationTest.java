package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.service.impl.UserServiceImpl;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserServiceImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AvailabilityFlowIntegrationTest {
    @Autowired UserService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    private AvailabilityHandler handler;
    private final ObjectMapper json = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url = System.getProperty("availability.test.jdbcUrl", "jdbc:h2:mem:availability214;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        boolean postgres = url.startsWith("jdbc:postgresql://127.0.0.1:") || url.startsWith("jdbc:postgresql://localhost:");
        if (!postgres && !url.startsWith("jdbc:h2:mem:")) throw new IllegalArgumentException("Use a disposable local database only");
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.driver-class-name", () -> postgres ? "org.postgresql.Driver" : "org.h2.Driver");
        properties.add("spring.datasource.username", () -> System.getProperty("availability.test.username", "sa"));
        properties.add("spring.datasource.password", () -> System.getProperty("availability.test.password", "test"));
        properties.add("spring.jpa.database-platform", () -> postgres ? "org.hibernate.dialect.PostgreSQLDialect" : "org.hibernate.dialect.H2Dialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeEach void setup() {
        jdbc.execute("DROP TABLE IF EXISTS user_availability");
        jdbc.execute("DROP TABLE IF EXISTS user_additional_details");
        jdbc.execute("DROP TABLE IF EXISTS users");
        new ResourceDatabasePopulator(new ClassPathResource("availability214-schema.sql")).execute(dataSource);
        jdbc.update("INSERT INTO users(user_id, full_name, primary_email_address, time_zone, language_1) VALUES (?, ?, ?, ?, ?)",
                "u1", "Local Test", "owner@example.org", "UTC", 123L);
        jdbc.update("INSERT INTO users(user_id, full_name, primary_email_address, time_zone) VALUES (?, ?, ?, ?)",
                "u2", "Other Test", "other@example.org", "UTC");
        MessageSourceUtil messages = mock(MessageSourceUtil.class);
        when(messages.getMessage(anyString(), any())).thenReturn("message");
        handler = new AvailabilityHandler(service, new ResponseBuilder(messages), messages);
    }

    private APIGatewayProxyRequestEvent event(String method, String body) {
        var context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(Map.of("claims", Map.of("email", "OWNER@example.org")));
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withRequestContext(context).withBody(body);
    }

    private String update(String timezone, String slots) {
        return "{\"userId\":\"u1\",\"timezone\":\"" + timezone + "\",\"slots\":" + slots
                + ",\"vacationMode\":true,\"vacationStartDate\":\"not-a-date\",\"vacationEndDate\":\"\"}";
    }

    @Test void browserPayloadRoundTripsThroughHandlerServiceAndDatabase() throws Exception {
        var response = handler.handleRequest(event("PUT", update("America/Chicago", """
                [{"frequency":"Weekdays","startTime":"09:00","endTime":"17:00"},
                 {"frequency":"Saturday","startTime":"22:00","endTime":"23:00"}]
                """)), null);
        assertThat(response.getStatusCode()).isEqualTo(200);
        JsonNode saved = json.readTree(response.getBody()).get("data");
        var fetched = handler.handleRequest(event("POST", "{\"userId\":\"u1\"}"), null);
        assertThat(json.readTree(fetched.getBody()).get("data")).isEqualTo(saved);
        assertThat(saved.get("slots")).hasSize(2);
        assertThat(saved.has("vacationMode")).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT time_zone FROM users WHERE user_id='u1'", String.class)).isEqualTo("America/Chicago");
        assertThat(jdbc.queryForObject("SELECT language_1 FROM users WHERE user_id='u1'", Long.class)).isEqualTo(123L);
        jdbc.query("SELECT day_of_week, start_time, end_time FROM user_availability", rs -> {
            LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
            LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
            assertThat(rs.getString("day_of_week")).isEqualToIgnoringCase(start.getDayOfWeek().name());
            assertThat(end).isAfter(start);
            var local = start.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("America/Chicago"));
            assertThat(local.getHour()).isIn(9, 22);
        });
        assertThat(handler.handleRequest(event("PUT", update("UTC", "[]")), null).getStatusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
    }

    @Test void deniedCallerCannotReadOrChangeAnotherAccount() {
        assertThat(handler.handleRequest(event("POST", "{\"userId\":\"u2\"}"), null).getStatusCode()).isEqualTo(403);
        assertThat(handler.handleRequest(event("PUT", update("UTC", "[]").replace("u1", "u2")), null).getStatusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isZero();
    }

    @Test void failedTimezoneWriteRollsBackScheduleAndTimezone() {
        String slot = "[{\"frequency\":\"Monday\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}]";
        assertThat(handler.handleRequest(event("PUT", update("UTC", slot)), null).getStatusCode()).isEqualTo(200);
        jdbc.execute("ALTER TABLE users ADD CONSTRAINT local_test_failure CHECK (time_zone <> 'Asia/Kolkata')");
        var failure = handler.handleRequest(event("PUT", "{\"userId\":\"u1\",\"timezone\":\"Asia/Kolkata\"}"), null);
        // The database rejects the timezone change, so the transaction must preserve both records.
        assertThat(failure.getStatusCode()).isEqualTo(500);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT day_of_week FROM user_availability", String.class)).isEqualTo("Monday");
        assertThat(jdbc.queryForObject("SELECT time_zone FROM users WHERE user_id='u1'", String.class)).isEqualTo("UTC");
    }

    @Test void timezoneOnlyUpdateConvertsSavedTimesAndPersistsThem() throws Exception {
        assertThat(handler.handleRequest(event("PUT", update("America/New_York",
                "[{\"frequency\":\"Monday\",\"startTime\":\"21:00\",\"endTime\":\"23:00\"}]")), null)
                .getStatusCode()).isEqualTo(200);
        var response = handler.handleRequest(event("PUT",
                "{\"userId\":\"u1\",\"timezone\":\"America/Chicago\"}"), null);
        assertThat(response.getStatusCode()).isEqualTo(200);
        var data = json.readTree(response.getBody()).get("data");
        assertThat(data.get("slots").get(0).get("startTime").asText()).isEqualTo("20:00");
        assertThat(data.get("slots").get(0).get("endTime").asText()).isEqualTo("22:00");
        var retry = handler.handleRequest(event("PUT",
                "{\"userId\":\"u1\",\"timezone\":\"America/Chicago\"}"), null);
        assertThat(retry.getStatusCode()).isEqualTo(200);
        assertThat(json.readTree(retry.getBody()).get("data")).isEqualTo(data);
        var fetched = handler.handleRequest(event("POST", "{\"userId\":\"u1\"}"), null);
        assertThat(json.readTree(fetched.getBody()).get("data")).isEqualTo(data);
    }

    @Test void failedTimezoneConversionLeavesSavedScheduleAndTimezoneUntouched() throws Exception {
        var initial = handler.handleRequest(event("PUT", update("UTC",
                "[{\"frequency\":\"Monday\",\"startTime\":\"18:00\",\"endTime\":\"19:00\"}]")), null);
        assertThat(initial.getStatusCode()).isEqualTo(200);
        var rejected = handler.handleRequest(event("PUT",
                "{\"userId\":\"u1\",\"timezone\":\"Asia/Kolkata\"}"), null);
        assertThat(rejected.getStatusCode()).isEqualTo(400);
        assertThat(json.readTree(rejected.getBody()).get("message").asText()).contains("crosses midnight");
        var fetched = handler.handleRequest(event("POST", "{\"userId\":\"u1\"}"), null);
        assertThat(json.readTree(fetched.getBody()).get("data"))
                .isEqualTo(json.readTree(initial.getBody()).get("data"));
    }

    @Test void invalidOvernightInputNeverChangesSavedAvailability() throws Exception {
        var initial = handler.handleRequest(event("PUT", update("UTC",
                "[{\"frequency\":\"Monday\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"}]")), null);
        for (String end : java.util.List.of("02:00", "21:30", "22:00", "00:00")) {
            var response = handler.handleRequest(event("PUT", update("UTC",
                    "[{\"frequency\":\"Monday\",\"startTime\":\"22:00\",\"endTime\":\"" + end + "\"}]")), null);
            assertThat(response.getStatusCode()).isEqualTo(400);
            assertThat(json.readTree(response.getBody()).get("message").asText()).contains("slots[0].endTime");
        }
        var fetched = handler.handleRequest(event("POST", "{\"userId\":\"u1\"}"), null);
        assertThat(json.readTree(fetched.getBody()).get("data"))
                .isEqualTo(json.readTree(initial.getBody()).get("data"));
    }

    @Test void failedInsertRollsBackDeletedSlots() {
        String slot = "[{\"frequency\":\"Monday\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}]";
        assertThat(handler.handleRequest(event("PUT", update("UTC", slot)), null).getStatusCode()).isEqualTo(200);
        jdbc.execute("ALTER TABLE user_availability ADD CONSTRAINT local_insert_failure CHECK (day_of_week <> 'Tuesday')");
        var failure = handler.handleRequest(event("PUT", update("UTC", slot.replace("Monday", "Tuesday"))), null);
        assertThat(failure.getStatusCode()).isEqualTo(500);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT day_of_week FROM user_availability", String.class)).isEqualTo("Monday");
    }

    @Test void timezoneChangeWithReplacementSlotsRequiresExplicitConversion() throws Exception {
        var initial = handler.handleRequest(event("PUT", update("UTC",
                "[{\"frequency\":\"Monday\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"}]")), null);
        var rejected = handler.handleRequest(event("PUT", update("America/Chicago",
                "[{\"frequency\":\"Monday\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"}]")), null);
        assertThat(rejected.getStatusCode()).isEqualTo(400);
        assertThat(json.readTree(rejected.getBody()).get("message").asText()).contains("omit slots");
        var fetched = handler.handleRequest(event("POST", "{\"userId\":\"u1\"}"), null);
        assertThat(json.readTree(fetched.getBody()).get("data"))
                .isEqualTo(json.readTree(initial.getBody()).get("data"));
    }

    @Test void timezoneOnlyUpdateSupportsEmptySchedulesAndChecksOwnership() throws Exception {
        var rejected = handler.handleRequest(event("PUT",
                "{\"userId\":\"u2\",\"timezone\":\"America/Chicago\"}"), null);
        assertThat(rejected.getStatusCode()).isEqualTo(403);
        var saved = handler.handleRequest(event("PUT",
                "{\"userId\":\"u1\",\"timezone\":\"America/Chicago\"}"), null);
        assertThat(saved.getStatusCode()).isEqualTo(200);
        assertThat(json.readTree(saved.getBody()).get("data").get("slots")).isEmpty();
        var fetched = handler.handleRequest(event("POST", "{\"userId\":\"u1\"}"), null);
        assertThat(json.readTree(fetched.getBody()).get("data").get("timezone").asText())
                .isEqualTo("America/Chicago");
    }

    @Test void concurrentReplacementsNeverLeaveMixedSchedules() throws Exception {
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        String first = "[{\"frequency\":\"Weekdays\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"}]";
        String second = "[{\"frequency\":\"Weekdays\",\"startTime\":\"14:00\",\"endTime\":\"16:00\"}]";
        try {
            var morning = executor.submit(() -> {
                start.await();
                return handler.handleRequest(event("PUT", update("UTC", first)), null).getStatusCode();
            });
            var afternoon = executor.submit(() -> {
                start.await();
                return handler.handleRequest(event("PUT", update("UTC", second)), null).getStatusCode();
            });
            start.countDown();
            assertThat(morning.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(afternoon.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(200);
            var fetched = handler.handleRequest(event("POST", "{\"userId\":\"u1\"}"), null);
            var slots = json.readTree(fetched.getBody()).get("data").get("slots");
            assertThat(slots).hasSize(1);
            assertThat(slots.get(0).get("startTime").asText()).isIn("09:00", "14:00");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isEqualTo(5);
        } finally {
            executor.shutdownNow();
        }
    }
}
