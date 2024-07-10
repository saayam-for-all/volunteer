package com.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sfa.volunteer.VolunteerApplication;
import com.sfa.volunteer.dto.PaginationResponse;
import com.sfa.volunteer.dto.UserProfileResponse;
import com.sfa.volunteer.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

public class GetAllUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
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
            Map<String, String> queryStringParameters = requestEvent.getQueryStringParameters();
            Integer page = null;
            Integer size = null;

            if (queryStringParameters != null) {
                if (queryStringParameters.containsKey("page")) {
                    page = Integer.parseInt(queryStringParameters.get("page"));
                }
                if (queryStringParameters.containsKey("size")) {
                    size = Integer.parseInt(queryStringParameters.get("size"));
                }
            }

            PaginationResponse<UserProfileResponse> paginationResponse = userService.findAllUsersWithPagination(page, size);

            String responseBody = objectMapper.writeValueAsString(paginationResponse);
            response.setBody(responseBody);
            response.setStatusCode(200); // OK
        } catch (Exception e) {
            response.setBody("Error: " + e.getMessage());
            response.setStatusCode(500); // Internal Server Error
        }

        return response;
    }
}
