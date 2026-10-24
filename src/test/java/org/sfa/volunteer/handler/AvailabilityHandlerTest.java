package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.response.AvailabilityResponse;
import org.sfa.volunteer.dto.response.AvailabilitySlotResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityHandlerTest {

    private static final String USER_ID = "SID-123";
    private static final String GET_BODY = "{\"userId\":\"SID-123\"}";
    private static final String PUT_BODY = """
            {
              "userId": "SID-123",
              "timezone": "America/Chicago",
              "slots": [{ "frequency": "Weekdays", "startTime": "09:00", "endTime": "17:00" }],
              "vacationMode": true,
              "vacationStartDate": "2026-12-20",
              "vacationEndDate": ""
            }
            """;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private UserService userService;
    @Mock
    private MessageSourceUtil messageSourceUtil;

    private AvailabilityHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(messageSourceUtil.getMessage(anyString(), any())).thenReturn("message");
        handler = new AvailabilityHandler(userService, new ResponseBuilder(messageSourceUtil), messageSourceUtil);
    }

    private static APIGatewayProxyRequestEvent request(String method, String body) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withBody(body);
    }

    private static JsonNode json(APIGatewayProxyResponseEvent response) throws Exception {
        return MAPPER.readTree(response.getBody());
    }

    private static AvailabilityResponse weekdays() {
        return new AvailabilityResponse(USER_ID, "America/Chicago",
                List.of(new AvailabilitySlotResponse(7, "Weekdays", "09:00", "17:00")),
                LocalDateTime.of(2026, 10, 23, 14, 5));
    }

    // POST: getAvailability

    @Test
    void postReturnsSavedAvailability() throws Exception {
        when(userService.getAvailability(USER_ID)).thenReturn(weekdays());

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", GET_BODY), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHeaders()).containsEntry("Access-Control-Allow-Methods", "POST,PUT,OPTIONS");
        JsonNode data = json(response).get("data");
        assertThat(data.get("timezone").asText()).isEqualTo("America/Chicago");
        assertThat(data.at("/slots/0/id").asInt()).isEqualTo(7);
        assertThat(data.at("/slots/0/frequency").asText()).isEqualTo("Weekdays");
        assertThat(data.at("/slots/0/startTime").asText()).isEqualTo("09:00");
        assertThat(data.at("/slots/0/endTime").asText()).isEqualTo("17:00");
        assertThat(data.get("lastUpdatedAt").asText()).isEqualTo("2026-10-23T14:05:00Z");
        verify(userService, never()).updateAvailability(any());
    }

    @Test
    void postReturnsEmptySlotsWhenNothingSaved() throws Exception {
        when(userService.getAvailability(USER_ID))
                .thenReturn(new AvailabilityResponse(USER_ID, "UTC", List.of(), null));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", GET_BODY), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        JsonNode slots = json(response).at("/data/slots");
        assertThat(slots.isArray()).isTrue();
        assertThat(slots).isEmpty();
    }

    @Test
    void decodesBase64Body() throws Exception {
        when(userService.getAvailability(USER_ID))
                .thenReturn(new AvailabilityResponse(USER_ID, "UTC", List.of(), null));
        String encoded = Base64.getEncoder().encodeToString(GET_BODY.getBytes(StandardCharsets.UTF_8));

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("POST", encoded).withIsBase64Encoded(true), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(json(response).at("/data/userId").asText()).isEqualTo(USER_ID);
    }

    // PUT: updateAvailability

    @Test
    void putSavesAvailabilityAndReturnsIt() throws Exception {
        when(userService.updateAvailability(any())).thenReturn(weekdays());

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("PUT", PUT_BODY), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(json(response).at("/data/slots/0/frequency").asText()).isEqualTo("Weekdays");

        ArgumentCaptor<UpdateAvailabilityRequest> captor = ArgumentCaptor.forClass(UpdateAvailabilityRequest.class);
        verify(userService).updateAvailability(captor.capture());
        UpdateAvailabilityRequest sent = captor.getValue();
        assertThat(sent.getUserId()).isEqualTo(USER_ID);
        assertThat(sent.getTimezone()).isEqualTo("America/Chicago");
        assertThat(sent.getSlots()).hasSize(1);
        assertThat(sent.getSlots().get(0).getStartTime()).isEqualTo("09:00");
        // accepted so the webapp can send its full payload
        assertThat(sent.getVacationMode()).isTrue();
        verify(userService, never()).getAvailability(any());
    }

    @Test
    void putReturnsServiceValidationMessageAs400() throws Exception {
        when(userService.updateAvailability(any()))
                .thenThrow(new IllegalArgumentException("slots[0].startTime must be HH:mm: 9am"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("PUT", PUT_BODY), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("slots[0].startTime must be HH:mm: 9am");
    }

    // shared

    @Test
    void answersPreflightWithoutCallingService() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("OPTIONS", null), null);

        assertThat(response.getStatusCode()).isEqualTo(204);
        assertThat(response.getHeaders()).containsEntry("Access-Control-Allow-Origin", "*");
        assertThat(response.getHeaders()).containsEntry("Access-Control-Allow-Methods", "POST,PUT,OPTIONS");
        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "DELETE", "PATCH"})
    void rejectsOtherMethods(String method) throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request(method, GET_BODY), null);

        assertThat(response.getStatusCode()).isEqualTo(405);
        assertThat(json(response).get("message").asText()).isEqualTo("Method not allowed: " + method);
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Origin");
        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsMissingUserId(String method) throws Exception {
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request(method, "{\"timezone\":\"UTC\",\"slots\":[]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("userId is required");
        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsMissingBody(String method) throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request(method, null), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("Request body is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsMalformedJson(String method) throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request(method, "{not json"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("Invalid request body");
    }

    @Test
    void postReturns404WhenUserDoesNotExist() throws Exception {
        when(userService.getAvailability(USER_ID)).thenThrow(new UserNotFoundException(USER_ID));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", GET_BODY), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(json(response).get("success").asBoolean()).isFalse();
    }

    @Test
    void putReturns404WhenUserDoesNotExist() throws Exception {
        when(userService.updateAvailability(any())).thenThrow(new UserNotFoundException(USER_ID));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("PUT", PUT_BODY), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(json(response).get("success").asBoolean()).isFalse();
    }

    @Test
    void returns500OnUnexpectedError() throws Exception {
        when(userService.getAvailability(USER_ID)).thenThrow(new IllegalStateException("db down"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", GET_BODY), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Origin");
    }
}
