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
import org.sfa.volunteer.dto.request.NotificationRequest;
import org.sfa.volunteer.dto.response.NotificationCountResponse;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Locale;
import java.util.Optional;

public class GetNotificationsCountHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final VolunteerService volunteerService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;
    private static final ObjectMapper objectMapper;

    static{
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        volunteerService = context.getBean(VolunteerService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try{
            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(headers -> headers.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);
            NotificationRequest bodyRequest = parseBodyRequest(requestEvent);
            String userId = Optional.ofNullable(requestEvent.getPathParameters())
                    .map(p -> p.get("userId"))
                    .filter(id -> !id.isBlank())
                    .orElse(bodyRequest != null ? bodyRequest.userId() : null);
            if (userId == null || userId.isBlank()) {
                throw new RuntimeException("Missing userId");
            }

            Long notificationsCount = volunteerService.getNotificationsCountAfterLastAccessed(userId);
            NotificationCountResponse notificationCountResponse = new NotificationCountResponse(notificationsCount);

            SaayamResponse<NotificationCountResponse> body = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[]{userId},
                    notificationCountResponse
            );

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(body));
        }
        catch (Exception e){
            String errMsg = messageSourceUtil.getMessage(SaayamStatusCode.USER_NOT_FOUND.getCode(), null);
            SaayamResponse<Void> errBody = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.USER_NOT_FOUND,
                    errMsg
            );
            response.setStatusCode(500);
            try {
                response.setBody(objectMapper.writeValueAsString(errBody));
            } catch (Exception ex) {
                response.setBody("{\"message\":\"Serialization error\"}");
            }
        }


        return response;
    }

    private NotificationRequest parseBodyRequest(APIGatewayProxyRequestEvent requestEvent) throws Exception {
        if (requestEvent.getBody() == null || requestEvent.getBody().isBlank()) {
            return null;
        }
        return objectMapper.readValue(requestEvent.getBody(), NotificationRequest.class);
    }
}
