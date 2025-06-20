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
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Optional;

public class UpdateOrganizationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;
    private static final ObjectMapper objectMapper;

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context ctx) {
        APIGatewayProxyResponseEvent resp = new APIGatewayProxyResponseEvent();
        try {
            String userId = Optional.ofNullable(req.getPathParameters())
                    .map(p -> p.get("userId"))
                    .orElseThrow(() -> new RuntimeException("Missing path parameter 'userId'"));

            Map<String, Object> bodyMap = objectMapper.readValue(req.getBody(), Map.class);
            UpdateOrganizationRequest updateReq = objectMapper.convertValue(bodyMap, UpdateOrganizationRequest.class);

            OrganizationResponse updated = userService.updateUserOrganization(userId, updateReq);

            SaayamResponse<OrganizationResponse> body = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.ORGANIZATION_UPDATED,
                    new Object[]{userId},
                    updated
            );

            resp.setStatusCode(200);
            resp.setBody(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            String errMsg = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
            SaayamResponse<Void> errBody = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    errMsg
            );
            resp.setStatusCode(500);
            try {
                resp.setBody(objectMapper.writeValueAsString(errBody));
            } catch (Exception ex) {
                resp.setBody("{\"message\":\"Serialization error\"}");
            }
        }
        return resp;
    }
}
