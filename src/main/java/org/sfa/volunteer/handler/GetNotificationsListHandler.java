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
import org.sfa.volunteer.dto.response.NotificationPaginationResponse;
import org.sfa.volunteer.dto.response.NotificationsResponse;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


public class GetNotificationsListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>  {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

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

            Map<String, String> queryStringParameters = requestEvent.getQueryStringParameters();
            NotificationRequest bodyRequest = parseBodyRequest(requestEvent);
            String userId = Optional.ofNullable(requestEvent.getPathParameters())
                    .map(p -> p.get("userId"))
                    .filter(id -> !id.isBlank())
                    .orElse(bodyRequest != null ? bodyRequest.userId() : null);
            if (userId == null || userId.isBlank()) {
                throw new RuntimeException("Missing userId");
            }

            Integer page = bodyRequest != null ? bodyRequest.page() : null;
            Integer size = bodyRequest != null ? bodyRequest.size() : null;
            LocalDateTime clientRefTime = bodyRequest != null ? bodyRequest.clientRefTime() : null;
            if (queryStringParameters != null) {
                if (page == null && queryStringParameters.containsKey("page")) {
                    page = Integer.parseInt(queryStringParameters.get("page"));
                }
                if (size == null && queryStringParameters.containsKey("size")) {
                    size = Integer.parseInt(queryStringParameters.get("size"));
                }
                if (clientRefTime == null && queryStringParameters.containsKey("clientRefTime")) {
                    clientRefTime = LocalDateTime.parse(queryStringParameters.get("clientRefTime"));
                }


            }
            int pageNumber = page != null ? page : DEFAULT_PAGE;
            int pageSize = size != null ? size : DEFAULT_SIZE;
            NotificationPaginationResponse<NotificationsResponse> paginationResponse = volunteerService.getNotificationsList(userId, pageNumber, pageSize, clientRefTime);

            SaayamResponse<NotificationPaginationResponse<NotificationsResponse>> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[]{},
                    paginationResponse
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(200); // OK
        }
        catch (Exception e){
            String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    errorMessage
            );

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                response.setBody(responseBody);
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(500); // Internal Server Error
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
