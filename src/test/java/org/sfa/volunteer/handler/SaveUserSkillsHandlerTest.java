package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveUserSkillsHandlerTest {

    @Mock private SkillsService skillsService;
    @Mock private ResponseBuilder responseBuilder;
    @Mock private MessageSourceUtil messageSourceUtil;

    private SaveUserSkillsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SaveUserSkillsHandler(skillsService, responseBuilder, messageSourceUtil);
    }

    private APIGatewayProxyRequestEvent event(String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody(body);
        return event;
    }

    @Test
    void handleRequest_returns200_onSuccess() {
        SaveUserSkillsResponse serviceResponse = SaveUserSkillsResponse.builder()
                .userId("user-1")
                .savedCatIds(List.of("1.1"))
                .wizardStage(3)
                .build();

        when(skillsService.saveUserSkills(any())).thenReturn(serviceResponse);
        doReturn(SaayamResponse.builder().success(true).statusCode(200).message("ok").data(serviceResponse).build())
                .when(responseBuilder).buildSuccessResponse(eq(SaayamStatusCode.USER_SKILLS_SAVED), any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-1\",\"catIds\":[\"1.1\"]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    void handleRequest_returns404_whenUserNotFound() {
        when(skillsService.saveUserSkills(any())).thenThrow(new UserNotFoundException("user-x"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("User not found");
        doReturn(SaayamResponse.builder().success(false).statusCode(404).message("User not found").build())
                .when(responseBuilder).buildErrorResponse(eq(404), eq(SaayamStatusCode.USER_NOT_FOUND), any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-x\",\"catIds\":[\"1.1\"]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    void handleRequest_returns400_whenSkillsException() {
        when(skillsService.saveUserSkills(any())).thenThrow(SkillsException.badRequest("user-1"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("Bad request");
        doReturn(SaayamResponse.builder().success(false).statusCode(400).message("Bad request").build())
                .when(responseBuilder).buildErrorResponse(eq(400), any(), any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-1\",\"catIds\":[]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    void handleRequest_returns500_onUnexpectedException() {
        when(skillsService.saveUserSkills(any())).thenThrow(new RuntimeException("DB down"));
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("Internal server error");
        doReturn(SaayamResponse.builder().success(false).statusCode(500).message("Internal server error").build())
                .when(responseBuilder).buildErrorResponse(eq(500), eq(SaayamStatusCode.INTERNAL_SERVER_ERROR), any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("{\"userId\":\"user-1\",\"catIds\":[\"1.1\"]}"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
    }

    @Test
    void handleRequest_returns500_whenBodyIsInvalidJson() {
        when(messageSourceUtil.getMessage(any(), isNull())).thenReturn("Internal server error");
        doReturn(SaayamResponse.builder().success(false).statusCode(500).message("Internal server error").build())
                .when(responseBuilder).buildErrorResponse(eq(500), eq(SaayamStatusCode.INTERNAL_SERVER_ERROR), any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(event("not-json"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
    }
}
