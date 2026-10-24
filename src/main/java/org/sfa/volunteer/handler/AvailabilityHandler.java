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
import org.sfa.volunteer.dto.request.AvailabilityUserRequest;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.response.AvailabilityResponse;
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
 * /availability, one Lambda for both operations; the user is always identified by the JSON body.
 * <ul>
 *   <li>POST {"userId": "SID-..."}: returns the user's weekly availability in their own timezone
 *       (an empty slot list when none is saved)</li>
 *   <li>PUT {"userId": "SID-...", "timezone": "America/Chicago", "slots": [...]}: replaces the user's
 *       availability and timezone, then returns what was saved</li>
 * </ul>
 */
@Slf4j
public class AvailabilityHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String ALLOWED_METHODS = "POST,PUT,OPTIONS";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Started once per Lambda container, on first use
    private static volatile ApplicationContext springContext;

    private final UserService userService;
    private final ResponseBuilder responseBuilder;
    private final MessageSourceUtil messageSourceUtil;

    public AvailabilityHandler() {
        this(bean(UserService.class), bean(ResponseBuilder.class), bean(MessageSourceUtil.class));
    }

    AvailabilityHandler(UserService userService, ResponseBuilder responseBuilder,
                         MessageSourceUtil messageSourceUtil) {
        this.userService = userService;
        this.responseBuilder = responseBuilder;
        this.messageSourceUtil = messageSourceUtil;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(Cors.headers(ALLOWED_METHODS));

        String method = requestEvent.getHttpMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return response.withStatusCode(204);
        }
        boolean isUpdate = "PUT".equalsIgnoreCase(method);
        if (!isUpdate && !"POST".equalsIgnoreCase(method)) {
            return error(response, 405, SaayamStatusCode.BAD_REQUEST, "Method not allowed: " + method);
        }

        try {
            String body = readBody(requestEvent);
            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("Request body is required");
            }

            String userId;
            AvailabilityResponse availability;
            if (isUpdate) {
                UpdateAvailabilityRequest request = OBJECT_MAPPER.readValue(body, UpdateAvailabilityRequest.class);
                userId = requireUserId(request.getUserId());
                availability = userService.updateAvailability(request);
            } else {
                AvailabilityUserRequest request = OBJECT_MAPPER.readValue(body, AvailabilityUserRequest.class);
                userId = requireUserId(request.getUserId());
                availability = userService.getAvailability(userId);
            }
            SaayamResponse<AvailabilityResponse> success =
                    responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{ userId }, availability);
            return response.withStatusCode(200).withBody(OBJECT_MAPPER.writeValueAsString(success));
        } catch (IllegalArgumentException e) {
            return error(response, 400, SaayamStatusCode.BAD_REQUEST, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("Invalid {} availability request body: {}", method, e.getOriginalMessage());
            return error(response, 400, SaayamStatusCode.BAD_REQUEST, "Invalid request body");
        } catch (UserNotFoundException e) {
            String msg = messageSourceUtil.getMessage(
                    SaayamStatusCode.USER_NOT_FOUND.getCode(), new Object[]{ e.getUserId() });
            return error(response, 404, SaayamStatusCode.USER_NOT_FOUND, msg);
        } catch (Exception e) {
            log.error("{} availability request failed", method, e);
            String msg = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
            return error(response, 500, SaayamStatusCode.INTERNAL_SERVER_ERROR, msg);
        }
    }

    private static String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId;
    }

    private static <T> T bean(Class<T> type) {
        ApplicationContext ctx = springContext;
        if (ctx == null) {
            synchronized (AvailabilityHandler.class) {
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
