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

    static {
        // Load the real Spring Boot application, not NotificationService.class: booting
        // from the service interface produces a context without the JPA repositories
        // the service depends on, so the handler fails on first invocation.
        // Same fix already applied to GetNotificationCountsHandler.
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        notificationService = context.getBean(NotificationService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

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

            response.setBody(objectMapper.writeValueAsString(successResponse));
            response.setStatusCode(200); // FIXED: 200 instead of 201

        } catch (Exception e) {

            SaayamStatusCode code = SaayamStatusCode.INTERNAL_SERVER_ERROR;
            String msg = messageSourceUtil.getMessage(code.getCode(), null);

            SaayamResponse<Void> error = responseBuilder.buildErrorResponse(
                    500,
                    code,
                    msg);

            try {
                response.setBody(objectMapper.writeValueAsString(error));
            } catch (Exception ignored) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(500);
        }

        return response;
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
