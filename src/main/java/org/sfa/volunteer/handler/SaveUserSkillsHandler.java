package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.SaveUserSkillsRequest;
import org.sfa.volunteer.dto.response.SaveUserSkillsResponse;
import org.sfa.volunteer.exception.SkillsException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.SkillsService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

public class SaveUserSkillsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final SkillsService skillsService;
    private final ResponseBuilder responseBuilder;
    private final MessageSourceUtil messageSourceUtil;

    public SaveUserSkillsHandler() {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        this.skillsService = context.getBean(SkillsService.class);
        this.responseBuilder = context.getBean(ResponseBuilder.class);
        this.messageSourceUtil = context.getBean(MessageSourceUtil.class);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    SaveUserSkillsHandler(SkillsService skillsService, ResponseBuilder responseBuilder, MessageSourceUtil messageSourceUtil, ObjectMapper objectMapper) {
        this.skillsService = skillsService;
        this.responseBuilder = responseBuilder;
        this.messageSourceUtil = messageSourceUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            SaveUserSkillsRequest request = objectMapper.readValue(requestEvent.getBody(), SaveUserSkillsRequest.class);

            SaveUserSkillsResponse savedSkills = skillsService.saveUserSkills(request);

            SaayamResponse<SaveUserSkillsResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.USER_SKILLS_SAVED,
                    savedSkills
            );

            response.setBody(objectMapper.writeValueAsString(successResponse));
            response.setStatusCode(200);
        } catch (UserNotFoundException e) {
            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    404,
                    SaayamStatusCode.USER_NOT_FOUND,
                    messageSourceUtil.getMessage(SaayamStatusCode.USER_NOT_FOUND.getCode(), null)
            );
            try {
                response.setBody(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }
            response.setStatusCode(404);
        } catch (SkillsException e) {
            SaayamStatusCode statusCode = SaayamStatusCode.valueOf(e.getMessage());
            String errorMessage = messageSourceUtil.getMessage(statusCode.getCode(), null);

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    400,
                    statusCode,
                    errorMessage
            );

            try {
                response.setBody(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }
            response.setStatusCode(400);
        } catch (Exception e) {
            String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    errorMessage
            );

            try {
                response.setBody(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }
            response.setStatusCode(500);
        }

        return response;
    }
}
