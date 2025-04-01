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
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class UpdateVolunteerHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final VolunteerService volunteerService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing for better readability
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        volunteerService = context.getBean(VolunteerService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
//            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(headers -> headers.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);

//            String userId = requestEvent.getPathParameters().get("userId");
            Map<String, Object> body = parseBody(requestEvent.getBody());
            VolunteerRequest updateRequest = parseRequest(body);

            VolunteerResponse updatedVolunteer = volunteerService.updateVolunteer(updateRequest);

            SaayamResponse<VolunteerResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.VOLUNTEER_UPDATED,
//                    new Object[]{userId},
                    new Object[]{updatedVolunteer.userId()},
                    updatedVolunteer
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(201); // Created
        } catch (Exception e) {
//            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(headers -> headers.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);

            String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
            int errorCode= 500;
            SaayamStatusCode saayamErrorMsg = SaayamStatusCode.INTERNAL_SERVER_ERROR;

            if (e.getMessage() != null) {
                saayamErrorMsg = SaayamStatusCode.valueOf(e.getMessage());
                errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.valueOf(e.getMessage()).getCode(), null);
            }

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    errorCode,
                    saayamErrorMsg,
                    errorMessage
            );

//            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
//                    500,
//                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
//                    errorMessage
//            );

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

    private VolunteerRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, VolunteerRequest.class);
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
