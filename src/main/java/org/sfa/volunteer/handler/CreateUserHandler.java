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
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.response.CreateUserResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Locale;
import java.util.Map;

public class CreateUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing for better readability
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);

            Map<String, Object> body = parseBody(requestEvent.getBody());
            CreateUserRequest createRequest = parseRequest(body);

            CreateUserResponse created = userService.createUser(createRequest);

            SaayamResponse<CreateUserResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.USER_CREATED,
                    new Object[]{created.userId()},
                    created
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(201); // Created
        } catch (Exception e) {
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);
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

    private CreateUserRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, CreateUserRequest.class);
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
