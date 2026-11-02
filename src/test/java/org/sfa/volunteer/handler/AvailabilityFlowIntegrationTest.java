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
                 {"frequency":"Saturday","startTime":"22:00","endTime":"02:00"}]
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

    @Test void failedInsertRollsBackDeletedSlotsAndTimezone() {
        String slot = "[{\"frequency\":\"Monday\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}]";
        assertThat(handler.handleRequest(event("PUT", update("UTC", slot)), null).getStatusCode()).isEqualTo(200);
        jdbc.execute("ALTER TABLE user_availability ADD CONSTRAINT local_test_failure CHECK (day_of_week <> 'Tuesday')");
        var failure = handler.handleRequest(event("PUT", update("Asia/Kolkata", slot.replace("Monday", "Tuesday"))), null);
        // Kolkata's Tuesday 09:00 remains Tuesday in UTC, so the constraint rejects insertion.
        assertThat(failure.getStatusCode()).isEqualTo(500);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_availability", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT day_of_week FROM user_availability", String.class)).isEqualTo("Monday");
        assertThat(jdbc.queryForObject("SELECT time_zone FROM users WHERE user_id='u1'", String.class)).isEqualTo("UTC");
    }
}
