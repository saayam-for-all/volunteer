package com.saayam.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.saayam.volunteer.VolunteerApplication;
import com.saayam.volunteer.domain.UpdateUserProfileRequest;
import com.saayam.volunteer.domain.UserProfileResponse;
import com.saayam.volunteer.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

public class UpdateUserProfileHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing for better readability

    static {
        ApplicationContext context = new AnnotationConfigApplicationContext(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String userId = requestEvent.getPathParameters().get("userId");
            Map<String, Object> body = parseBody(requestEvent.getBody());
            UpdateUserProfileRequest updateRequest = parseRequest(body);
            UserProfileResponse updatedProfile = userService.updateUserProfile(userId, updateRequest);
            String responseBody = objectMapper.writeValueAsString(updatedProfile);
            response.setBody(responseBody);
            response.setStatusCode(200); // OK
        } catch (Exception e) {
            response.setBody("Error: " + e.getMessage());
            response.setStatusCode(500); // Internal Server Error
        }

        return response;
    }

    private UpdateUserProfileRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, UpdateUserProfileRequest.class);
    }

    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse request body", e);
        }
    }
}
