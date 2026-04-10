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
import org.sfa.volunteer.dto.request.UpdatePersonalInfoRequest;
import org.sfa.volunteer.dto.response.PersonalInfoResponse;
import org.sfa.volunteer.exception.CountryNotFoundException;
import org.sfa.volunteer.exception.StateNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public class UpdatePersonalInfoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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
            // Parse request body
            Map<String, Object> body = parseBody(requestEvent.getBody());
            UpdatePersonalInfoRequest updateRequest = parseRequest(body);

            // Validate userId is provided
            if (updateRequest.getUserId() == null || updateRequest.getUserId().trim().isEmpty()) {
                throw new IllegalArgumentException("User ID is required");
            }

            // Update personal info
            PersonalInfoResponse updatedInfo = userService.updatePersonalInfo(updateRequest);

            // Build success response
            SaayamResponse<PersonalInfoResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.USER_PERSONAL_INFO_UPDATED,
                    new Object[]{updateRequest.getUserId()},
                    updatedInfo
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

        } catch (StateNotFoundException e) {
            context.getLogger().log("State not found: " + e.getMessage());

            String errorMessage = messageSourceUtil.getMessage(
                    SaayamStatusCode.STATE_NOT_FOUND.getCode(),
                    new Object[]{e.getStateName()}
            );

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    404,
                    SaayamStatusCode.STATE_NOT_FOUND,
                    errorMessage
            );

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                response.setBody(responseBody);
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(404); // Not Found

        } catch (CountryNotFoundException e) {
            context.getLogger().log("Country not found: " + e.getMessage());

            String errorMessage = messageSourceUtil.getMessage(
                    SaayamStatusCode.COUNTRY_NOT_FOUND.getCode(),
                    new Object[]{e.getCountryName()}
            );

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    404,
                    SaayamStatusCode.COUNTRY_NOT_FOUND,
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
                    e.getMessage()
            );

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                response.setBody(responseBody);
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(400); // Bad Request

        } catch (Exception e) {
            context.getLogger().log("UpdatePersonalInfo error: " + e.getMessage());

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

    private UpdatePersonalInfoRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, UpdatePersonalInfoRequest.class);
    }

    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse request body", e);
        }
    }
}