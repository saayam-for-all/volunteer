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
import org.sfa.volunteer.dto.response.AddressStatusResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

public class GetUserAddressStatusHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final UserService userService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;
    private static final ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            String userId = Optional.ofNullable(request.getPathParameters())
                    .map(p -> p.get("userId"))
                    .orElseThrow(() -> new RuntimeException("Missing path parameter 'userId'"));

            AddressStatusResponse addressStatusResponse = userService.getAddressStatus(userId);
            SaayamResponse<AddressStatusResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[]{},
                    addressStatusResponse
            );
            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(200);
        } catch (Exception e) {
            String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
            SaayamResponse<Void> errBody = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    errorMessage
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

}