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
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.exception.UserNotFoundException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class GetNotificationDetailsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final NotificationService notificationService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing for better readability
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    // --- CORS headers ---
    private static final Map<String, String> CORS_HEADERS = new HashMap<>();
    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        notificationService = context.getBean(NotificationService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);

        CORS_HEADERS.put("Access-Control-Allow-Origin", "*");
        CORS_HEADERS.put("Access-Control-Allow-Methods", "POST,OPTIONS");
        CORS_HEADERS.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent eResponse = new APIGatewayProxyResponseEvent().withHeaders(CORS_HEADERS);

        // Preflight
        if ("OPTIONS".equalsIgnoreCase(requestEvent.getHttpMethod())) {
            return eResponse.withStatusCode(204);
        }
        try {
            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(headers -> headers.getOrDefault("Accept-Language", "en"))
                    .orElse("en");

            Locale locale = Locale.forLanguageTag(lang);

            Map<String, Object> body = parseBody(requestEvent.getBody());
            GetNotificationsRequest request = parseRequest(body);
            String userId = request.userId();

            GetNotificationsResponse notificationsResponse = notificationService.getNotifications(request);

            SaayamResponse<GetNotificationsResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[] { userId },
                    notificationsResponse);

            String responseBody = objectMapper.writeValueAsString(successResponse);
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

        SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
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
