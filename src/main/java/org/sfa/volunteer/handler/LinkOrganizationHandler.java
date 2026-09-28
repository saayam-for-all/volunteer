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
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.sfa.volunteer.exception.OrganizationNotFoundException;

import java.util.Map;
import java.util.Optional;

public class LinkOrganizationHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final OrganizationService organizationService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    static {
        ApplicationContext applicationContext =
                SpringApplication.run(VolunteerApplication.class);

        organizationService =
                applicationContext.getBean(OrganizationService.class);

        responseBuilder =
                applicationContext.getBean(ResponseBuilder.class);

        messageSourceUtil =
                applicationContext.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent requestEvent,
            Context context
    ) {
        APIGatewayProxyResponseEvent response =
                new APIGatewayProxyResponseEvent();

        try {
            Map<String, String> pathParameters =
                    Optional.ofNullable(
                            requestEvent.getPathParameters()
                    ).orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Path parameters are required"
                            )
                    );

            String userId = Optional.ofNullable(
                            pathParameters.get("userId")
                    )
                    .filter(value -> !value.isBlank())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Missing path parameter 'userId'"
                            )
                    );

            String orgId = Optional.ofNullable(
                            pathParameters.get("orgId")
                    )
                    .filter(value -> !value.isBlank())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Missing path parameter 'orgId'"
                            )
                    );

            organizationService.linkOrganization(userId, orgId);

            LinkOrganizationResponse responseData =
                    new LinkOrganizationResponse(
                            userId,
                            orgId,
                            "User successfully linked to organization"
                    );

            SaayamResponse<LinkOrganizationResponse> successResponse =
                    responseBuilder.buildSuccessResponse(
                            SaayamStatusCode.ORGANIZATION_UPDATED,
                            new Object[]{userId, orgId},
                            responseData
                    );

            response.setStatusCode(201);
            response.setBody(
                    objectMapper.writeValueAsString(successResponse)
            );

        } catch (IllegalArgumentException e) {
            setErrorResponse(response, 400, SaayamStatusCode.BAD_REQUEST);
        } catch (OrganizationNotFoundException e) {
            setErrorResponse(response, 404, SaayamStatusCode.ORGANIZATION_NOT_FOUND);
        } catch (Exception e) {
            setErrorResponse(response, 500, SaayamStatusCode.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    private void setErrorResponse(
            APIGatewayProxyResponseEvent response,
            int httpStatus,
            SaayamStatusCode saayamStatusCode
    ) {
        String errorMessage = messageSourceUtil.getMessage(
                saayamStatusCode.getCode(),
                null
        );

        SaayamResponse<Void> errorResponse =
                responseBuilder.buildErrorResponse(
                        httpStatus,
                        saayamStatusCode,
                        errorMessage
                );

        response.setStatusCode(httpStatus);

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

    private record LinkOrganizationResponse(
            String userId,
            String orgId,
            String message
    ) {
    }
}