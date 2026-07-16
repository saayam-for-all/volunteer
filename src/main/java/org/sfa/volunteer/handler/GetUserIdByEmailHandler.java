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
import org.sfa.volunteer.dto.request.FindUserProfileUsingEmail;
import org.sfa.volunteer.dto.response.UserIdResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class GetUserIdByEmailHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    // --- CORS headers ---
    private static final Map<String, String> CORS_HEADERS = new HashMap<>();
    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);

        CORS_HEADERS.put("Access-Control-Allow-Origin", "*");
        CORS_HEADERS.put("Access-Control-Allow-Methods", "POST,OPTIONS");
        CORS_HEADERS.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(CORS_HEADERS);

        // Preflight
        if (requestEvent.getHttpMethod() != null && requestEvent.getHttpMethod().equalsIgnoreCase("OPTIONS")) {
            return response.withStatusCode(204);
        }

        try {
            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(h -> h.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);

            Map<String, Object> body = parseBody(requestEvent.getBody());
            FindUserProfileUsingEmail req = parseRequest(body);

            UserIdResponse userId = userService.getUserIdByEmail(req.email());

            SaayamResponse<UserIdResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[]{ req.email() },
                    userId
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            return response.withStatusCode(200).withBody(responseBody);

        } catch (UserNotFoundException unfe) {
            String msg = messageSourceUtil.getMessage(
                    SaayamStatusCode.USER_NOT_FOUND.getCode(),
                    new Object[]{ unfe.getUserId() }
            );

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    404,
                    SaayamStatusCode.USER_NOT_FOUND,
                    msg
            );

            String responseBody;
            try {
                responseBody = objectMapper.writeValueAsString(errorResponse);
            } catch (Exception jsonException) {
                responseBody = "{\"message\":\"Failed to serialize error response\"}";
            }
            return response.withStatusCode(404).withBody(responseBody);

        } catch (Exception e) {
            String msg = messageSourceUtil.getMessage(
                    SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    null
            );

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    msg
            );

            String responseBody;
            try {
                responseBody = objectMapper.writeValueAsString(errorResponse);
            } catch (Exception jsonException) {
                responseBody = "{\"message\":\"Failed to serialize error response\"}";
            }
            return response.withStatusCode(500).withBody(responseBody);
        }
    }

    private FindUserProfileUsingEmail parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, FindUserProfileUsingEmail.class);
    }

    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse request body", e);
        }
    }
}
