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
import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import jakarta.validation.Validator;

public class CreateOrganizationHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final OrganizationService organizationService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;
    private static final Validator validator;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    static {
        ApplicationContext applicationContext =
                SpringApplication.run(VolunteerApplication.class);

        organizationService = applicationContext.getBean(OrganizationService.class);

        responseBuilder = applicationContext.getBean(ResponseBuilder.class);

        messageSourceUtil = applicationContext.getBean(MessageSourceUtil.class);
        
       validator = applicationContext.getBean(Validator.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent requestEvent,
            Context context
    ) {
        APIGatewayProxyResponseEvent response =
                new APIGatewayProxyResponseEvent();

        try {
            if (requestEvent.getBody() == null
                    || requestEvent.getBody().isBlank()) {
                throw new IllegalArgumentException(
                        "Request body is required"
                );
            }

            CreateOrganizationRequest createRequest =
                    objectMapper.readValue(
                            requestEvent.getBody(),
                            CreateOrganizationRequest.class
                    );
            var violations = validator.validate(createRequest);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(v -> v.getMessage())
                        .collect(java.util.stream.Collectors.joining("; "));
                throw new IllegalArgumentException(errorMessage);
             }

            OrganizationResponse createdOrganization = organizationService.createOrganization(createRequest);

            SaayamResponse<OrganizationResponse> successResponse =
                    responseBuilder.buildSuccessResponse(
                            SaayamStatusCode.ORGANIZATION_CREATED,
                            new Object[]{createdOrganization.orgId()},
                            createdOrganization
                    );

            response.setStatusCode(201);
            response.setBody(
                    objectMapper.writeValueAsString(successResponse)
            );

        } catch (IllegalArgumentException e) {
            setErrorResponse(
                    response,
                    400,
                    SaayamStatusCode.BAD_REQUEST
            );

        } catch (Exception e) {
            setErrorResponse(
                    response,
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR
            );
        }

        return response;
    }

    private void setErrorResponse(
            APIGatewayProxyResponseEvent response,
            int statusCode,
            SaayamStatusCode saayamStatusCode
    ) {
        String errorMessage = messageSourceUtil.getMessage(
                saayamStatusCode.getCode(),
                null
        );

        SaayamResponse<Void> errorResponse =
                responseBuilder.buildErrorResponse(
                        statusCode,
                        saayamStatusCode,
                        errorMessage
                );

        response.setStatusCode(statusCode);

        try {
            response.setBody(
                    objectMapper.writeValueAsString(errorResponse)
            );
        } catch (Exception jsonException) {
            response.setBody(
                    "{\"message\":\"Failed to serialize error response\"}"
            );
        }
    }
}