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
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.exception.WizardProgressMutationException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Locale;
import java.util.Map;

public class UpdateUserProfileHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final UserService userService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing for better readability
    private final ResponseBuilder responseBuilder;
    private final MessageSourceUtil messageSourceUtil;

    public UpdateUserProfileHandler() {
        this(ProfileContext.INSTANCE.getBean(UserService.class), ProfileContext.INSTANCE.getBean(ResponseBuilder.class),
                ProfileContext.INSTANCE.getBean(MessageSourceUtil.class));
    }

    UpdateUserProfileHandler(UserService userService, ResponseBuilder responseBuilder, MessageSourceUtil messageSourceUtil) {
        this.userService = userService;
        this.responseBuilder = responseBuilder;
        this.messageSourceUtil = messageSourceUtil;
    }

    private static final class ProfileContext {
        private static final ApplicationContext INSTANCE = new AnnotationConfigApplicationContext(VolunteerApplication.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);

            String userId = requestEvent.getPathParameters().get("userId");
            Map<String, Object> body = parseBody(requestEvent.getBody());
            UpdateUserProfileRequest updateRequest = parseRequest(body);

            UserProfileResponse updatedProfile = userService.updateUserProfile(userId, updateRequest);

            SaayamResponse<UserProfileResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.USER_ACCOUNT_UPDATED,
                    new Object[]{userId},
                    updatedProfile
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(200); // OK
        } catch (Exception e) {
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);
            boolean invalidProgress = e instanceof WizardProgressMutationException;
            int status = invalidProgress ? 400 : 500;
            SaayamStatusCode code = invalidProgress ? SaayamStatusCode.BAD_REQUEST : SaayamStatusCode.INTERNAL_SERVER_ERROR;
            String errorMessage = invalidProgress ? e.getMessage() : messageSourceUtil.getMessage(code.getCode(), null);

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    status,
                    code,
                    errorMessage
            );

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                response.setBody(responseBody);
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(status);
        }

        return response;
    }

    private UpdateUserProfileRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, UpdateUserProfileRequest.class);
    }

    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse request body", e);
        }
    }
}
