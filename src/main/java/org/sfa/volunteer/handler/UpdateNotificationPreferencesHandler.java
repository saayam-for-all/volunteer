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
import org.sfa.volunteer.dto.request.UpdateNotificationPreferencesRequest;
import org.sfa.volunteer.dto.response.UpdateNotificationPreferencesResponse;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.service.NotificationPreferenceService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class UpdateNotificationPreferencesHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final NotificationPreferenceService notificationPreferenceService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    private static final Map<String, String> CORS_HEADERS = new HashMap<>();

    static {
        // Boot the whole application, not the service interface: the context has to
        // carry the JPA repositories this handler's service depends on.
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        notificationPreferenceService = context.getBean(NotificationPreferenceService.class);
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
            Map<String, Object> body = parseBody(requestEvent.getBody());
            UpdateNotificationPreferencesRequest request = objectMapper.convertValue(body, UpdateNotificationPreferencesRequest.class);

            UpdateNotificationPreferencesResponse result = notificationPreferenceService.updatePreferences(request);

            SaayamResponse<UpdateNotificationPreferencesResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[] { request.userId() },
                    result);

            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(successResponse));

        } catch (NotificationException ne) {
            return response.withStatusCode(400)
                    .withBody(serialise(400, SaayamStatusCode.BAD_REQUEST));
        } catch (Exception e) {
            return response.withStatusCode(500)
                    .withBody(serialise(500, SaayamStatusCode.INTERNAL_SERVER_ERROR));
        }
    }

    private String serialise(int status, SaayamStatusCode code) {
        String msg = messageSourceUtil.getMessage(code.getCode(), null);
        SaayamResponse<Void> error = responseBuilder.buildErrorResponse(status, code, msg);
        try {
            return objectMapper.writeValueAsString(error);
        } catch (Exception ignored) {
            return "{\"message\":\"Failed to serialize error response\"}";
        }
    }

    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new NotificationException(SaayamStatusCode.BAD_REQUEST.toString(), null);
        }
    }
}
