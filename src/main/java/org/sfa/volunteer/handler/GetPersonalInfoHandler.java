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
import org.sfa.volunteer.dto.request.PersonalInfoRequest;
import org.sfa.volunteer.dto.response.PersonalInfoResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

public class GetPersonalInfoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            // Extract request body and parse to PersonalInfoRequest
            String requestBody = requestEvent.getBody();

            if (requestBody == null || requestBody.trim().isEmpty()) {
                throw new IllegalArgumentException("Request body is required");
            }

            // Deserialize request body to PersonalInfoRequest
            PersonalInfoRequest request = objectMapper.readValue(requestBody, PersonalInfoRequest.class);

            // Validate userId (manual validation since @Valid doesn't work in Lambda)
            if (request.userId() == null || request.userId().trim().isEmpty()) {
                throw new IllegalArgumentException("userId is required");
            }

            // Get personal info
            PersonalInfoResponse personalInfo = userService.getPersonalInfoById(request.userId());

            // Build success response
            SaayamResponse<PersonalInfoResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[]{request.userId()},
                    personalInfo
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(200); // OK

        } catch (UserNotFoundException e) {
            context.getLogger().log("User not found: " + e.getMessage());

            String errorMessage = messageSourceUtil.getMessage(
                    SaayamStatusCode.USER_NOT_FOUND.getCode(),
                    new Object[]{e.getUserId()}
            );

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    404,
                    SaayamStatusCode.USER_NOT_FOUND,
                    errorMessage
            );

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                response.setBody(responseBody);
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(404); // Not Found

        } catch (IllegalArgumentException e) {
            context.getLogger().log("Invalid request: " + e.getMessage());

            String errorMessage = messageSourceUtil.getMessage(
                    SaayamStatusCode.INVALID_PARAMETER.getCode(),
                    null
            );

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    400,
                    SaayamStatusCode.INVALID_PARAMETER,
                    errorMessage
            );

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                response.setBody(responseBody);
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(400); // Bad Request

        } catch (Exception e) {
            context.getLogger().log("GetPersonalInfo error: " + e.getMessage());

            String errorMessage = messageSourceUtil.getMessage(
                    SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    null
            );

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    errorMessage
            );

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                response.setBody(responseBody);
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(500); // Internal Server Error
        }

        return response;
    }
}