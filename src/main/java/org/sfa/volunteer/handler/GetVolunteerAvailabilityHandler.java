// handler/GetVolunteerAvailabilityHandler.java
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
import org.sfa.volunteer.dto.response.VolunteerAvailabilityResponse;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class GetVolunteerAvailabilityHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final VolunteerService volunteerService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        volunteerService = context.getBean(VolunteerService.class);
        responseBuilder  = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent requestEvent, Context context) {

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            // Language handling
            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(h -> h.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);

            // Extract userId from path parameters
            Map<String, String> pathParams = requestEvent.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("userId")) {
                throw new IllegalArgumentException("userId path parameter is required");
            }

            String userId = pathParams.get("userId");
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("userId cannot be blank");
            }

            // Call service — returns empty list on first visit
            VolunteerAvailabilityResponse availabilityResponse =
                    volunteerService.getVolunteerUserAvailability(userId);

            SaayamResponse<VolunteerAvailabilityResponse> successResponse =
                    responseBuilder.buildSuccessResponse(
                            SaayamStatusCode.SUCCESS,
                            new Object[]{userId},
                            availabilityResponse
                    );

            response.setBody(objectMapper.writeValueAsString(successResponse));
            response.setStatusCode(200);

        } catch (IllegalArgumentException e) {
            context.getLogger().log("Validation error: " + e.getMessage());

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
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
            context.getLogger().log("Error fetching availability: " + e.getMessage());
            e.printStackTrace();

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    messageSourceUtil.getMessage(
                            SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null)
            );

            try {
                response.setBody(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception ex) {
                response.setBody("{\"message\":\"Internal server error\"}");
            }
            response.setStatusCode(500);
        }

        return response;
    }
}
