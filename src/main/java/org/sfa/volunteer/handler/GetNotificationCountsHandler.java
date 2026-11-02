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
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.NotificationException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class GetNotificationCountsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final NotificationService notificationService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    
        // --- CORS headers ---
    private static final Map<String, String> CORS_HEADERS = new HashMap<>();
    static {
        // FIXED: load your real Spring Boot application, not NotificationService.class
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        notificationService = context.getBean(NotificationService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);

        CORS_HEADERS.put("Access-Control-Allow-Origin", "*");
        CORS_HEADERS.put("Access-Control-Allow-Methods", "POST,OPTIONS");
        CORS_HEADERS.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent eResponse = new APIGatewayProxyResponseEvent().withHeaders(CORS_HEADERS);

        // Preflight request
        if ("OPTIONS".equalsIgnoreCase(event.getHttpMethod())) {
        return eResponse.withStatusCode(204);
        }

        try {

            String lang = Optional.ofNullable(event.getHeaders())
                    .map(h -> h.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);
            // FIXED: direct deserialization instead of Map parsing
            GetNotificationsRequest request = objectMapper.readValue(event.getBody(), GetNotificationsRequest.class);

            GetNotificationsResponse result = notificationService.getNotificationCounts(request);

            SaayamResponse<GetNotificationsResponse> success = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[] { request.userId() },
                    result);

            String responseBody = objectMapper.writeValueAsString(success);
            return eResponse.withStatusCode(200).withBody(responseBody);

        } catch (NotificationException ex) {

            if ("USER_NOT_FOUND".equals(ex.getMessage())) {
            String msg = messageSourceUtil.getMessage(
                    SaayamStatusCode.USER_NOT_FOUND.getCode(),
                    new Object[] { ex.getUserId() });

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    404,
                    SaayamStatusCode.USER_NOT_FOUND,
                    msg);

            String responseBody;
            try {
                responseBody = objectMapper.writeValueAsString(errorResponse);
            } catch (Exception jsonException) {
                responseBody = "{\"message\":\"USER_NOT_FOUND\"}";
            }
            return eResponse.withStatusCode(404).withBody(responseBody);
        } 
            return buildInternalServerErrorResponse(eResponse);

        } catch (Exception e) {

            return buildInternalServerErrorResponse(eResponse);
        }
    }

    private APIGatewayProxyResponseEvent buildInternalServerErrorResponse(
        APIGatewayProxyResponseEvent response) {

                SaayamStatusCode code = SaayamStatusCode.INTERNAL_SERVER_ERROR;

            String msg = messageSourceUtil.getMessage(
                        code.getCode(),
                        null);

            SaayamResponse<Void> errorResponse =
                responseBuilder.buildErrorResponse(
                    500,
                    code,
                    msg);

            String responseBody;
            try {
                responseBody = objectMapper.writeValueAsString(errorResponse);
            } catch (Exception ex) {
                responseBody = "{\"message\":\"INTERNAL_SERVER_ERROR\"}";
            }

            return response
                    .withStatusCode(500)
                    .withBody(responseBody);
        }

    private GetNotificationsRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, GetNotificationsRequest.class);
    }

    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            // todo: define a customized error
            throw new RuntimeException("Failed to parse request body", e);
        }
    }

}
