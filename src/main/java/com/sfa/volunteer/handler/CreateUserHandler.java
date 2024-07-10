package com.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sfa.volunteer.VolunteerApplication;
import com.sfa.volunteer.dto.CreateUserRequest;
import com.sfa.volunteer.dto.CreateUserResponse;
import com.sfa.volunteer.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;
import java.util.Map;

public class CreateUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing for better readability
    private static final ResourceBundleMessageSource messageSource;

    static {
        ApplicationContext context = new AnnotationConfigApplicationContext(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        messageSource = context.getBean(ResourceBundleMessageSource.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        LambdaLogger logger = context.getLogger();

        try {
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);

            logger.log("Received request body: " + requestEvent.getBody());
            Map<String, Object> body = parseBody(requestEvent.getBody());
            CreateUserRequest createRequest = parseRequest(body);

            CreateUserResponse created = userService.createUser(createRequest, locale);

            String responseBody = objectMapper.writeValueAsString(created);
            logger.log("Created response body: " + responseBody);
            response.setBody(responseBody);
            response.setStatusCode(201); // Created
        } catch (Exception e) {
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);
            String errorMessage = messageSource.getMessage("error.internalServerError", null, locale);

            logger.log("Error: " + e.getMessage());
            response.setBody("Error: " + errorMessage);
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
            throw new RuntimeException("Failed to parse request body", e);
        }
    }
}