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
import org.sfa.volunteer.dto.request.SignOffRequest;
import org.sfa.volunteer.dto.response.SignOffResponse;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SignOffUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final UserService userService;
    private static final ProfileImageStorageService profileImageStorageService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        profileImageStorageService = context.getBean(ProfileImageStorageService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            // Language handling
            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(h -> h.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);

            // Body is required
            if (requestEvent.getBody() == null || requestEvent.getBody().isBlank()) {
                throw new IllegalArgumentException("Request body is required");
            }

            // Deserialize body directly into DTO
            SignOffRequest signOffRequest =
                    objectMapper.readValue(requestEvent.getBody(), SignOffRequest.class);

            // userId validation (mandatory)
            if (signOffRequest.userId() == null || signOffRequest.userId().isBlank()) {
                throw new IllegalArgumentException("userId is required");
            }

            String userId = signOffRequest.userId();
            String reason = signOffRequest.reason(); // optional

            // Delete profile image
            if (userService.getProfilePicturePath(userId).isPresent()) {
                profileImageStorageService.delete(userId, "us-east-1");
            }

            // Sign off user
            SignOffResponse signOffResponse =
                    userService.signOffUser(userId, reason);

            // Build success response
            SaayamResponse<SignOffResponse> successResponse =
                    responseBuilder.buildSuccessResponse(
                            SaayamStatusCode.USER_DELETED,
                            new Object[]{userId},
                            signOffResponse
                    );

            response.setBody(objectMapper.writeValueAsString(successResponse));
            response.setStatusCode(200);

        } catch (IllegalArgumentException e) {
            context.getLogger().log("Validation error: " + e.getMessage());

            SaayamResponse<Void> errorResponse =
                    responseBuilder.buildErrorResponse(
                            400,
                            SaayamStatusCode.BAD_REQUEST,
                            e.getMessage()
                    );

            try {
                response.setBody(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception ex) {
                response.setBody("{\"message\":\"Invalid request\"}");
            }

            response.setStatusCode(400);

        } catch (Exception e) {
            context.getLogger().log("Error processing sign-off request: " + e.getMessage());
            e.printStackTrace();

            SaayamResponse<Void> errorResponse =
                    responseBuilder.buildErrorResponse(
                            500,
                            SaayamStatusCode.INTERNAL_SERVER_ERROR,
                            messageSourceUtil.getMessage(
                                    SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null)
                    );

            try {
                response.setBody(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception ex) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(500);
        }

        return response;
    }

}