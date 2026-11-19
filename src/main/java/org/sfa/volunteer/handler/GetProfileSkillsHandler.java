package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.UserSkillsRequest;
import org.sfa.volunteer.dto.response.UserSkillsResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.Cors;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * POST /profileSkills  body: {"userId": "SID-..."}
 * Returns the user's saved skills; an empty list when the user has none.
 */
@Slf4j
public class GetProfileSkillsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String ALLOWED_METHODS = "POST,OPTIONS";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Started once per Lambda container, on first use
    private static volatile ApplicationContext springContext;

    private final UserService userService;
    private final ResponseBuilder responseBuilder;
    private final MessageSourceUtil messageSourceUtil;

    public GetProfileSkillsHandler() {
        this(bean(UserService.class), bean(ResponseBuilder.class), bean(MessageSourceUtil.class));
    }

    GetProfileSkillsHandler(UserService userService, ResponseBuilder responseBuilder,
                         MessageSourceUtil messageSourceUtil) {
        this.userService = userService;
        this.responseBuilder = responseBuilder;
        this.messageSourceUtil = messageSourceUtil;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(Cors.headers(ALLOWED_METHODS));

        if ("OPTIONS".equalsIgnoreCase(requestEvent.getHttpMethod())) {
            return response.withStatusCode(204);
        }

        try {
            String body = readBody(requestEvent);
            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("Request body is required");
            }
            UserSkillsRequest request = OBJECT_MAPPER.readValue(body, UserSkillsRequest.class);
            String userId = request.getUserId();
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("userId is required");
            }

            UserSkillsResponse skills = userService.getUserSkills(userId);
            SaayamResponse<UserSkillsResponse> success =
                    responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{ userId }, skills);
            return response.withStatusCode(200).withBody(OBJECT_MAPPER.writeValueAsString(success));
        } catch (IllegalArgumentException e) {
            return error(response, 400, SaayamStatusCode.BAD_REQUEST, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("Invalid user skills request body: {}", e.getOriginalMessage());
            return error(response, 400, SaayamStatusCode.BAD_REQUEST, "Invalid request body");
        } catch (UserNotFoundException e) {
            String msg = messageSourceUtil.getMessage(
                    SaayamStatusCode.USER_NOT_FOUND.getCode(), new Object[]{ e.getUserId() });
            return error(response, 404, SaayamStatusCode.USER_NOT_FOUND, msg);
        } catch (Exception e) {
            log.error("User skills request failed", e);
            String msg = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
            return error(response, 500, SaayamStatusCode.INTERNAL_SERVER_ERROR, msg);
        }
    }

    private static <T> T bean(Class<T> type) {
        ApplicationContext ctx = springContext;
        if (ctx == null) {
            synchronized (GetProfileSkillsHandler.class) {
                if (springContext == null) {
                    springContext = SpringApplication.run(VolunteerApplication.class);
                }
                ctx = springContext;
            }
        }
        return ctx.getBean(type);
    }

    private static String readBody(APIGatewayProxyRequestEvent requestEvent) {
        String body = requestEvent.getBody();
        if (body != null && Boolean.TRUE.equals(requestEvent.getIsBase64Encoded())) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        return body;
    }

    private APIGatewayProxyResponseEvent error(APIGatewayProxyResponseEvent response, int status,
                                               SaayamStatusCode code, String message) {
        SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(status, code, message);
        try {
            return response.withStatusCode(status).withBody(OBJECT_MAPPER.writeValueAsString(errorResponse));
        } catch (JsonProcessingException jsonException) {
            return response.withStatusCode(status).withBody("{\"message\":\"Failed to serialize error response\"}");
        }
    }
}
