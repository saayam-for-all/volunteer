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
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.*;

public class GetUserProfileByEmailHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;
    private static final ObjectMapper objectMapper;
    private static final Map<String, String> CORS_HEADERS = new HashMap<>();

    public GetUserProfileByEmailHandler() {
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(CORS_HEADERS);

        Map<String, String> headers =
                Optional.ofNullable(requestEvent.getHeaders()).orElse(Collections.emptyMap());

        String lang = headers.getOrDefault("Accept-Language", "en");
        Locale locale = Locale.forLanguageTag(lang);

        try {
            String email =
                    Optional.ofNullable(requestEvent.getPathParameters())
                            .map(m -> m.get("email"))
                            .orElseThrow(() -> new RuntimeException("Missing 'email' path parameter"));

            UserProfileResponse profile = userService.getUserProfileByEmail(email);

            SaayamResponse<UserProfileResponse> successResponse =
                    responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[] { email }, profile);

            String body = objectMapper.writeValueAsString(successResponse);
            return response.withStatusCode(200).withBody(body);

        } catch (UserNotFoundException unfe) {
            String userId = unfe.getUserId();

            String localizedMessage =
                    messageSourceUtil.getMessage(
                            SaayamStatusCode.USER_NOT_FOUND.getCode(),
                            new Object[] { userId }
                    );

            SaayamResponse<Void> errorResponse =
                    responseBuilder.buildErrorResponse(404, SaayamStatusCode.USER_NOT_FOUND, localizedMessage);

            String body;
            try {
                body = objectMapper.writeValueAsString(errorResponse);
            } catch (Exception e) {
                body = "{\"message\":\"Serialization error\"}";
            }

            return response.withStatusCode(404).withBody(body);

        } catch (Exception e) {
            String localizedMessage =
                    messageSourceUtil.getMessage(
                            SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(),
                            null
                    );

            SaayamResponse<Void> errorResponse =
                    responseBuilder.buildErrorResponse(500, SaayamStatusCode.INTERNAL_SERVER_ERROR, localizedMessage);

            String body;
            try {
                body = objectMapper.writeValueAsString(errorResponse);
            } catch (Exception ex) {
                body = "{\"message\":\"Serialization error\"}";
            }

            return response.withStatusCode(500).withBody(body);
        }
    }

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class, new String[0]);
        userService = context.getBean(UserService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        CORS_HEADERS.put("Access-Control-Allow-Origin", "*");
        CORS_HEADERS.put("Access-Control-Allow-Methods", "GET,OPTIONS,PUT");
        CORS_HEADERS.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }
}
