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
import org.sfa.volunteer.dto.request.CheckUserExistsRequest;
import org.sfa.volunteer.dto.response.UserExistsResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CheckUserExistsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;
    private static final Map<String, String> CORS_HEADERS = new HashMap<>();

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);

        CORS_HEADERS.put("Access-Control-Allow-Origin", "*");
        CORS_HEADERS.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        CORS_HEADERS.put("Access-Control-Allow-Headers", "Content-Type, Accept-Language");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(CORS_HEADERS);

        Map<String, String> headers =
                Optional.ofNullable(requestEvent.getHeaders()).orElse(Collections.emptyMap());

        String lang = headers.getOrDefault("Accept-Language", "en");
        Locale locale = Locale.forLanguageTag(lang);
        LocaleContextHolder.setLocale(locale);

        try {
            Map<String, Object> body = parseBody(requestEvent.getBody());
            CheckUserExistsRequest checkRequest = objectMapper.convertValue(body, CheckUserExistsRequest.class);
            validateRequiredFields(checkRequest);

            UserExistsResponse result = userService.checkUserExists(checkRequest);

            SaayamResponse<UserExistsResponse> successResponse =
                    responseBuilder.buildSuccessResponse(SaayamStatusCode.USER_EXISTS_CHECK, result);

            String responseBody = objectMapper.writeValueAsString(successResponse);
            return response.withStatusCode(200).withBody(responseBody);

            } catch (IllegalArgumentException e) {
                SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    400,
                    SaayamStatusCode.BAD_REQUEST,
                    e.getMessage()
                );
                try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                return response.withStatusCode(400).withBody(responseBody);
                } catch (Exception jsonException) {
                return response.withStatusCode(400)
                    .withBody("{\"message\":\"Failed to serialize error response\"}");
                }

        } catch (Exception e) {
            String errorMessage = messageSourceUtil.getMessage(
                    SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    500, SaayamStatusCode.INTERNAL_SERVER_ERROR, errorMessage);

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                return response.withStatusCode(500).withBody(responseBody);
            } catch (Exception jsonException) {
                return response.withStatusCode(500)
                        .withBody("{\"message\":\"Failed to serialize error response\"}");
            }
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    private Map<String, Object> parseBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Request body is required");
        }
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid request body format", e);
        }
    }

    private void validateRequiredFields(CheckUserExistsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request payload is required");
        }
        if (request.firstName() == null || request.firstName().trim().isEmpty()) {
            throw new IllegalArgumentException("firstName is required");
        }
        if (request.lastName() == null || request.lastName().trim().isEmpty()) {
            throw new IllegalArgumentException("lastName is required");
        }
    }
}
