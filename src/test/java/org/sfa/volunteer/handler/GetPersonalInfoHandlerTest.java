package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.response.PersonalInfoResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GetPersonalInfoHandler.
 *
 * "Unit" means nothing real is started: no Spring, no database. The service is a Mockito mock
 * (a stand-in we can program with when(...).thenReturn(...)), and the handler is built through
 * its package-private constructor, which is why this test lives in the same package as the handler.
 */
@ExtendWith(MockitoExtension.class)
class GetPersonalInfoHandlerTest {

    private static final String USER_ID = "SID-123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private UserService userService;
    @Mock
    private MessageSourceUtil messageSourceUtil;

    private GetPersonalInfoHandler handler;

    @BeforeEach
    void setUp() {
        // lenient(): don't fail tests that never ask for a message
        lenient().when(messageSourceUtil.getMessage(anyString(), any())).thenReturn("message");
        handler = new GetPersonalInfoHandler(userService, new ResponseBuilder(messageSourceUtil), messageSourceUtil);
    }

    private static APIGatewayProxyRequestEvent request(String method, String body) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withBody(body);
    }

    private static JsonNode json(APIGatewayProxyResponseEvent response) throws Exception {
        return MAPPER.readTree(response.getBody());
    }

    private static PersonalInfoResponse sampleInfo() {
        return PersonalInfoResponse.builder()
                .dateOfBirth("05/12/1990")
                .gender("Female")
                .addressLine1("1 Main St")
                .city("San Jose")
                .state("California")
                .country("United States")
                .zipCode("95112")
                .secondaryEmail("alt@example.com")
                .build();
    }

    @Test
    void returnsPersonalInfoForKnownUser() throws Exception {
        when(userService.getPersonalInfoById(USER_ID)).thenReturn(sampleInfo());

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"SID-123\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.at("/data/dateOfBirth").asText()).isEqualTo("05/12/1990");
        assertThat(body.at("/data/state").asText()).isEqualTo("California");
        assertThat(body.at("/data/secondaryEmail").asText()).isEqualTo("alt@example.com");
    }

    @Test
    void includesCorsHeadersOnSuccess() {
        when(userService.getPersonalInfoById(USER_ID)).thenReturn(sampleInfo());

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"SID-123\"}"), null);

        assertThat(response.getHeaders())
                .containsEntry("Access-Control-Allow-Origin", "*")
                .containsEntry("Access-Control-Allow-Methods", "POST,OPTIONS");
    }

    @Test
    void decodesBase64Body() {
        when(userService.getPersonalInfoById(USER_ID)).thenReturn(sampleInfo());
        String encoded = Base64.getEncoder()
                .encodeToString("{\"userId\":\"SID-123\"}".getBytes(StandardCharsets.UTF_8));

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("POST", encoded).withIsBase64Encoded(true), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    void answersPreflightWithoutCallingService() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("OPTIONS", null), null);

        assertThat(response.getStatusCode()).isEqualTo(204);
        assertThat(response.getHeaders()).containsEntry("Access-Control-Allow-Origin", "*");
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsMissingUserId() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", "{}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("userId is required");
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsBlankUserId() throws Exception {
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"   \"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("userId is required");
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsMissingBody() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", null), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("Request body is required");
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsMalformedJsonWith400NotServerError() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", "{not json"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("Invalid request body");
        verifyNoInteractions(userService);
    }

    @Test
    void returns404WhenUserDoesNotExist() throws Exception {
        when(userService.getPersonalInfoById("SID-404")).thenThrow(new UserNotFoundException("SID-404"));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"SID-404\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        JsonNode body = json(response);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("saayamCode").asText()).isEqualTo("SAAAYAM-1302");
    }

    @Test
    void returns500OnUnexpectedFailure() throws Exception {
        when(userService.getPersonalInfoById(USER_ID)).thenThrow(new IllegalStateException("db down"));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"SID-123\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(json(response).get("success").asBoolean()).isFalse();
        // internal details must not leak to the client
        assertThat(response.getBody()).doesNotContain("db down");
    }
}
