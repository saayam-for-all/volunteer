package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.SaveUserSkillsRequest;
import org.sfa.volunteer.dto.response.SaveUserSkillsResponse;
import org.sfa.volunteer.exception.SkillsException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.SkillsService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveUserSkillsHandlerTest {

    @Mock private SkillsService skillsService;
    @Mock private ResponseBuilder responseBuilder;
    @Mock private MessageSourceUtil messageSourceUtil;
    @Mock private ObjectMapper objectMapper;

    private SaveUserSkillsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SaveUserSkillsHandler(skillsService, responseBuilder, messageSourceUtil, objectMapper);
    }

    private APIGatewayProxyRequestEvent event(String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody(body);
        return event;
    }

    @Test
    void handleRequest_returns200_onSuccess() throws Exception {
        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1").catIds(List.of("1.1")).build();
        SaveUserSkillsResponse serviceResponse = SaveUserSkillsResponse.builder()
                .userId("user-1").savedCatIds(List.of("1.1")).wizardStage(3).build();

        when(objectMapper.readValue(any(String.class), eq(SaveUserSkillsRequest.class))).thenReturn(request);
        when(skillsService.saveUserSkills(any())).thenReturn(serviceResponse);
        doReturn(SaayamResponse.builder().success(true).statusCode(200).message("ok").data(serviceResponse).build())
                .when(responseBuilder).buildSuccessResponse(eq(SaayamStatusCode.USER_SKILLS_SAVED), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"success\":true}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-1\",\"catIds\":[\"1.1\"]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    void handleRequest_returns404_whenUserNotFound() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(SaveUserSkillsRequest.class)))
                .thenReturn(SaveUserSkillsRequest.builder().userId("user-x").catIds(List.of("1.1")).build());
        when(skillsService.saveUserSkills(any())).thenThrow(new UserNotFoundException("user-x"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("User not found");
        doReturn(SaayamResponse.builder().success(false).statusCode(404).message("User not found").build())
                .when(responseBuilder).buildErrorResponse(eq(404), eq(SaayamStatusCode.USER_NOT_FOUND), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"success\":false}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-x\",\"catIds\":[\"1.1\"]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    void handleRequest_returns400_whenSkillsException() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(SaveUserSkillsRequest.class)))
                .thenReturn(SaveUserSkillsRequest.builder().userId("user-1").catIds(List.of()).build());
        when(skillsService.saveUserSkills(any())).thenThrow(SkillsException.badRequest("user-1"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("Bad request");
        doReturn(SaayamResponse.builder().success(false).statusCode(400).message("Bad request").build())
                .when(responseBuilder).buildErrorResponse(eq(400), any(), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"success\":false}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-1\",\"catIds\":[]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    void handleRequest_returns500_onUnexpectedException() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(SaveUserSkillsRequest.class)))
                .thenReturn(SaveUserSkillsRequest.builder().userId("user-1").catIds(List.of("1.1")).build());
        when(skillsService.saveUserSkills(any())).thenThrow(new RuntimeException("DB down"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("Internal server error");
        doReturn(SaayamResponse.builder().success(false).statusCode(500).message("Internal server error").build())
                .when(responseBuilder).buildErrorResponse(eq(500), eq(SaayamStatusCode.INTERNAL_SERVER_ERROR), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"success\":false}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-1\",\"catIds\":[\"1.1\"]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
    }

    @Test
    void handleRequest_returns500_whenBodyIsInvalidJson() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(SaveUserSkillsRequest.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "bad json"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("Internal server error");
        doReturn(SaayamResponse.builder().success(false).statusCode(500).message("Internal server error").build())
                .when(responseBuilder).buildErrorResponse(eq(500), eq(SaayamStatusCode.INTERNAL_SERVER_ERROR), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"success\":false}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event("not-json"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
    }

    @Test
    void handleRequest_fallsBackToErrorString_whenUserNotFoundSerializationFails() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(SaveUserSkillsRequest.class)))
                .thenReturn(SaveUserSkillsRequest.builder().userId("user-x").catIds(List.of("1.1")).build());
        when(skillsService.saveUserSkills(any())).thenThrow(new UserNotFoundException("user-x"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("User not found");
        doReturn(SaayamResponse.builder().success(false).statusCode(404).message("User not found").build())
                .when(responseBuilder).buildErrorResponse(eq(404), eq(SaayamStatusCode.USER_NOT_FOUND), any());
        doThrow(new JsonProcessingException("serialize fail") {}).when(objectMapper).writeValueAsString(any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-x\",\"catIds\":[\"1.1\"]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getBody()).contains("Failed to serialize");
    }

    @Test
    void handleRequest_fallsBackToErrorString_whenSkillsExceptionSerializationFails() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(SaveUserSkillsRequest.class)))
                .thenReturn(SaveUserSkillsRequest.builder().userId("user-1").catIds(List.of()).build());
        when(skillsService.saveUserSkills(any())).thenThrow(SkillsException.badRequest("user-1"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("Bad request");
        doReturn(SaayamResponse.builder().success(false).statusCode(400).message("Bad request").build())
                .when(responseBuilder).buildErrorResponse(eq(400), any(), any());
        doThrow(new JsonProcessingException("serialize fail") {}).when(objectMapper).writeValueAsString(any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-1\",\"catIds\":[]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Failed to serialize");
    }

    @Test
    void handleRequest_fallsBackToErrorString_whenGenericExceptionSerializationFails() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(SaveUserSkillsRequest.class)))
                .thenReturn(SaveUserSkillsRequest.builder().userId("user-1").catIds(List.of("1.1")).build());
        when(skillsService.saveUserSkills(any())).thenThrow(new RuntimeException("DB down"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("Internal server error");
        doReturn(SaayamResponse.builder().success(false).statusCode(500).message("Internal server error").build())
                .when(responseBuilder).buildErrorResponse(eq(500), eq(SaayamStatusCode.INTERNAL_SERVER_ERROR), any());
        doThrow(new JsonProcessingException("serialize fail") {}).when(objectMapper).writeValueAsString(any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-1\",\"catIds\":[\"1.1\"]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).contains("Failed to serialize");
    }
}
