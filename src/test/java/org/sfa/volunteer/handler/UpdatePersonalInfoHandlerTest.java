package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.UpdatePersonalInfoRequest;
import org.sfa.volunteer.dto.response.PersonalInfoResponse;
import org.sfa.volunteer.exception.CountryNotFoundException;
import org.sfa.volunteer.exception.StateNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UpdatePersonalInfoHandler. Same approach as GetPersonalInfoHandlerTest:
 * the service is a Mockito mock, so we test only what the handler itself decides
 * (validation, status codes, headers), not the database logic.
 */
@ExtendWith(MockitoExtension.class)
class UpdatePersonalInfoHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private UserService userService;
    @Mock
    private MessageSourceUtil messageSourceUtil;

    private UpdatePersonalInfoHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(messageSourceUtil.getMessage(anyString(), any())).thenReturn("message");
        handler = new UpdatePersonalInfoHandler(userService, new ResponseBuilder(messageSourceUtil), messageSourceUtil);
    }

    private static APIGatewayProxyRequestEvent request(String method, String body) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withBody(body);
    }

    private static JsonNode json(APIGatewayProxyResponseEvent response) throws Exception {
        return MAPPER.readTree(response.getBody());
    }

    private static PersonalInfoResponse savedInfo() {
        return PersonalInfoResponse.builder()
                .dateOfBirth("05/12/1990")
                .city("San Jose")
                .state("California")
                .country("United States")
                .build();
    }

    // ---------- success ----------

    @Test
    void updatesAndReturnsSavedPersonalInfo() throws Exception {
        when(userService.updatePersonalInfo(any(UpdatePersonalInfoRequest.class))).thenReturn(savedInfo());

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("PUT",
                "{\"userId\":\"SID-1\",\"dateOfBirth\":\"05/12/1990\",\"city\":\"San Jose\","
                        + "\"state\":\"California\",\"country\":\"United States\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("saayamCode").asText()).isEqualTo("SAAAYAM-1204");
        assertThat(body.at("/data/city").asText()).isEqualTo("San Jose");

        // the JSON body must arrive in the service as a populated request object
        ArgumentCaptor<UpdatePersonalInfoRequest> captor = ArgumentCaptor.forClass(UpdatePersonalInfoRequest.class);
        verify(userService).updatePersonalInfo(captor.capture());
        UpdatePersonalInfoRequest sent = captor.getValue();
        assertThat(sent.getUserId()).isEqualTo("SID-1");
        assertThat(sent.getDateOfBirth()).isEqualTo("05/12/1990");
        assertThat(sent.getState()).isEqualTo("California");
        assertThat(sent.getGender()).isNull();   // omitted fields stay null = "leave unchanged"
    }

    @Test
    void ignoresUnknownJsonFields() {
        when(userService.updatePersonalInfo(any(UpdatePersonalInfoRequest.class))).thenReturn(savedInfo());

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("PUT",
                "{\"userId\":\"SID-1\",\"city\":\"San Jose\",\"somethingNew\":true}"), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    void includesCorsHeaders() {
        when(userService.updatePersonalInfo(any(UpdatePersonalInfoRequest.class))).thenReturn(savedInfo());

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("PUT", "{\"userId\":\"SID-1\"}"), null);

        assertThat(response.getHeaders())
                .containsEntry("Access-Control-Allow-Origin", "*")
                .containsEntry("Access-Control-Allow-Methods", "PUT,OPTIONS");
    }

    @Test
    void answersPreflightWithoutCallingService() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("OPTIONS", null), null);

        assertThat(response.getStatusCode()).isEqualTo(204);
        assertThat(response.getHeaders()).containsEntry("Access-Control-Allow-Origin", "*");
        verifyNoInteractions(userService);
    }

    // ---------- validation ----------

    @Test
    void rejectsMissingUserId() throws Exception {
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("PUT", "{\"city\":\"San Jose\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("User ID is required");
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsBlankUserId() throws Exception {
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("PUT", "{\"userId\":\"  \",\"city\":\"San Jose\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("User ID is required");
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsMissingBody() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("PUT", null), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("Request body is required");
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsMalformedJsonWith400NotServerError() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("PUT", "{not json"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("Invalid request body");
        verifyNoInteractions(userService);
    }

    @Test
    void returns400WhenServiceRejectsDateOfBirthFormat() throws Exception {
        when(userService.updatePersonalInfo(any(UpdatePersonalInfoRequest.class)))
                .thenThrow(new IllegalArgumentException("dateOfBirth must be in MM/dd/yyyy format"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request("PUT",
                "{\"userId\":\"SID-1\",\"dateOfBirth\":\"1990-05-12\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("dateOfBirth must be in MM/dd/yyyy format");
    }

    // ---------- not found / failures ----------

    @Test
    void returns404WhenUserDoesNotExist() throws Exception {
        when(userService.updatePersonalInfo(any(UpdatePersonalInfoRequest.class)))
                .thenThrow(new UserNotFoundException("SID-404"));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("PUT", "{\"userId\":\"SID-404\",\"city\":\"X\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(json(response).get("saayamCode").asText()).isEqualTo("SAAAYAM-1302");
    }

    @Test
    void returns404WhenStateDoesNotExist() throws Exception {
        when(userService.updatePersonalInfo(any(UpdatePersonalInfoRequest.class)))
                .thenThrow(new StateNotFoundException("Atlantis"));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("PUT", "{\"userId\":\"SID-1\",\"state\":\"Atlantis\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(json(response).get("saayamCode").asText()).isEqualTo("SAAAYAM-1307");
    }

    @Test
    void returns404WhenCountryDoesNotExist() throws Exception {
        when(userService.updatePersonalInfo(any(UpdatePersonalInfoRequest.class)))
                .thenThrow(new CountryNotFoundException("Narnia"));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("PUT", "{\"userId\":\"SID-1\",\"country\":\"Narnia\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(json(response).get("saayamCode").asText()).isEqualTo("SAAAYAM-1306");
    }

    @Test
    void returns500OnUnexpectedFailure() throws Exception {
        when(userService.updatePersonalInfo(any(UpdatePersonalInfoRequest.class)))
                .thenThrow(new IllegalStateException("db down"));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("PUT", "{\"userId\":\"SID-1\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).doesNotContain("db down");
    }
}
