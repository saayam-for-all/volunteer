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
import org.sfa.volunteer.dto.response.UserSkillsResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.SkillLevel;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProfileSkillsHandlerTest {

    private static final String USER_ID = "SID-123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private UserService userService;
    @Mock
    private MessageSourceUtil messageSourceUtil;

    private GetProfileSkillsHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(messageSourceUtil.getMessage(anyString(), any())).thenReturn("message");
        handler = new GetProfileSkillsHandler(userService, new ResponseBuilder(messageSourceUtil), messageSourceUtil);
    }

    private static APIGatewayProxyRequestEvent request(String method, String body) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withBody(body);
    }

    private static JsonNode json(APIGatewayProxyResponseEvent response) throws Exception {
        return MAPPER.readTree(response.getBody());
    }

    private static UserSkillsResponse savedSkills(String... catIds) {
        Map<String, SkillLevel> levels = new LinkedHashMap<>();
        for (String catId : catIds) {
            levels.put(catId, null);
        }
        return new UserSkillsResponse(USER_ID, List.of(catIds), levels);
    }


    @Test
    void returnsSavedSkillsAsCategoryIds() throws Exception {
        Map<String, SkillLevel> levels = new LinkedHashMap<>();
        levels.put("4.2", SkillLevel.ADVANCED);
        levels.put("0.1", null);
        when(userService.getUserSkills(USER_ID))
                .thenReturn(new UserSkillsResponse(USER_ID, List.of("4.2", "0.1"), levels));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"SID-123\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHeaders()).containsEntry("Access-Control-Allow-Methods", "POST,OPTIONS");
        JsonNode body = json(response);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.at("/data/skills/0").asText()).isEqualTo("4.2");
        assertThat(body.at("/data/skills/1").asText()).isEqualTo("0.1");
        assertThat(body.at("/data/skillLevels/4.2").asText()).isEqualTo("ADVANCED");
    }

    @Test
    void returnsEmptyListWhenUserHasNoSkills() throws Exception {
        when(userService.getUserSkills(USER_ID)).thenReturn(savedSkills());

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"SID-123\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        JsonNode skills = json(response).at("/data/skills");
        assertThat(skills.isArray()).isTrue();
        assertThat(skills).isEmpty();
    }

    @Test
    void decodesBase64Body() throws Exception {
        when(userService.getUserSkills(USER_ID)).thenReturn(savedSkills("4.2"));
        String encoded = Base64.getEncoder()
                .encodeToString("{\"userId\":\"SID-123\"}".getBytes(StandardCharsets.UTF_8));

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("POST", encoded).withIsBase64Encoded(true), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(json(response).at("/data/skills/0").asText()).isEqualTo("4.2");
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
    void rejectsMissingBody() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", null), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("Request body is required");
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("POST", "{not json"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).isEqualTo("Invalid request body");
    }

    @Test
    void returns404WhenUserDoesNotExist() throws Exception {
        when(userService.getUserSkills(USER_ID)).thenThrow(new UserNotFoundException(USER_ID));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"SID-123\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(json(response).get("success").asBoolean()).isFalse();
    }

    @Test
    void returns500OnUnexpectedError() throws Exception {
        when(userService.getUserSkills(USER_ID)).thenThrow(new IllegalStateException("db down"));

        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "{\"userId\":\"SID-123\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Origin");
    }
}
