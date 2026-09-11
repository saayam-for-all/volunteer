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
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;

public class SearchOrganizationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final OrganizationService organizationService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);

        organizationService = context.getBean(OrganizationService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent requestEvent,
            Context context
    ) {

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String name = Optional.ofNullable(requestEvent.getQueryStringParameters())
                    .map(params -> params.get("name"))
                    .orElseThrow(() -> new RuntimeException("Missing organization name"));

            List<OrganizationResponse> organizations =
                    organizationService.searchByName(name);

            SaayamResponse<List<OrganizationResponse>> successResponse =
                    responseBuilder.buildSuccessResponse(
                            SaayamStatusCode.SUCCESS,
                            new Object[]{name},
                            organizations
                    );

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(successResponse));

        } catch (Exception e) {

            String errorMessage = messageSourceUtil.getMessage(
                    SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    null
            );

            SaayamResponse<Void> errorResponse =
                    responseBuilder.buildErrorResponse(
                            500,
                            SaayamStatusCode.INTERNAL_SERVER_ERROR,
                            errorMessage
                    );

            try {
                response.setBody(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(500);
        }

        return response;
    }
}