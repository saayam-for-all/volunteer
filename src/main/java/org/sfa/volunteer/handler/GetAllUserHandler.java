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
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;
import java.util.Map;

public class GetAllUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
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

        try {
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);

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

            SaayamResponse<PaginationResponse<UserProfileResponse>> successResponse = ResponseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    messageSource.getMessage(SaayamStatusCode.SUCCESS.getCode(), null, locale),
                    paginationResponse
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(200); // OK
        } catch (Exception e) {
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);
            String errorMessage = messageSource.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null, locale);

            SaayamResponse<Void> errorResponse = ResponseBuilder.buildErrorResponse(
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
}
