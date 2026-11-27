package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.NotificationActionResponse;
import org.sfa.volunteer.enums.ActionStatus;
import org.sfa.volunteer.exception.NotificationAcceptException;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.AvailabilityAuthorization;
import org.sfa.volunteer.util.Cors;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** API Gateway REST API (payload v1) adapter for the shared notification actions. */
public abstract class NotificationActionHandlerBase
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final boolean accept;
    private final NotificationService notifications;
    private final UserService users;
    private final ResponseBuilder responses;
    private final MessageSourceUtil messages;

    protected NotificationActionHandlerBase(boolean accept) {
        this(accept, SpringContext.INSTANCE.getBean(NotificationService.class),
                SpringContext.INSTANCE.getBean(UserService.class),
                SpringContext.INSTANCE.getBean(ResponseBuilder.class),
                SpringContext.INSTANCE.getBean(MessageSourceUtil.class));
    }

    protected NotificationActionHandlerBase(boolean accept, NotificationService notifications, UserService users,
                                            ResponseBuilder responses, MessageSourceUtil messages) {
        this.accept = accept;
        this.notifications = notifications;
        this.users = users;
        this.responses = responses;
        this.messages = messages;
    }

    private static class SpringContext {
        // Shared for warm invocations; no embedded HTTP server is needed by a proxy-event handler.
        private static final ApplicationContext INSTANCE = new SpringApplicationBuilder(VolunteerApplication.class)
                .web(WebApplicationType.NONE).run();
    }

    @Override
    public final APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        var headers = Cors.headers("POST,OPTIONS");
        headers.compute("Access-Control-Allow-Headers", (key, value) -> value + ",X-User-Id");
        var response = new APIGatewayProxyResponseEvent().withHeaders(headers);
        if (event == null) return error(response, 400, SaayamStatusCode.BAD_REQUEST, null);
        if ("OPTIONS".equalsIgnoreCase(event.getHttpMethod())) return response.withStatusCode(204);
        if (!"POST".equalsIgnoreCase(event.getHttpMethod())) {
            headers.put("Allow", "POST,OPTIONS");
            return error(response, 405, SaayamStatusCode.BAD_REQUEST, null);
        }

        try {
            String email = callerEmail(event);
            if (email == null || email.isBlank()) {
                return error(response, 401, SaayamStatusCode.UNAUTHORIZED, null);
            }
            Integer id = notificationId(event);
            String userId = header(event, "X-User-Id");
            if (userId == null || userId.isBlank()) {
                return error(response, 400, SaayamStatusCode.BAD_REQUEST, null);
            }
            // A browser-provided ID is a selector, not proof of identity. Only trusted
            // Gateway claims authorize it; never decode an unverified Authorization token here.
            AvailabilityAuthorization.requireOwner(users, userId, email);
            NotificationActionResponse result = accept
                    ? notifications.acceptNotification(id, userId) : notifications.denyNotification(id, userId);
            if (result.actionStatus() == ActionStatus.REQUEST_TAKEN) {
                return response.withStatusCode(409).withBody(JSON.writeValueAsString(responses.buildErrorResponse(
                        409, SaayamStatusCode.NOTIFICATION_REQUEST_TAKEN, null, result)));
            }
            SaayamStatusCode code = accept ? SaayamStatusCode.NOTIFICATION_ACCEPTED : SaayamStatusCode.NOTIFICATION_DENIED;
            return response.withStatusCode(200).withBody(JSON.writeValueAsString(responses.buildSuccessResponse(code, result)));
        } catch (NotificationAcceptException ex) {
            return error(response, ex.getHttpStatus().value(), ex.getStatusCode(), new Object[]{ex.getDetail()});
        } catch (NotificationException ex) {
            SaayamStatusCode code = switch (String.valueOf(ex.getMessage())) {
                case "NOTIFICATION_NOT_FOUND" -> SaayamStatusCode.NOTIFICATION_NOT_FOUND;
                case "NOTIFICATION_ALREADY_ACTIONED" -> SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED;
                case "USER_NOT_FOUND" -> SaayamStatusCode.USER_NOT_FOUND;
                case "BAD_REQUEST", "INVALID_PARAMETER" -> SaayamStatusCode.BAD_REQUEST;
                default -> SaayamStatusCode.INTERNAL_SERVER_ERROR;
            };
            int status = switch (code) {
                case NOTIFICATION_NOT_FOUND, USER_NOT_FOUND -> 404;
                case NOTIFICATION_ALREADY_ACTIONED -> 409;
                case BAD_REQUEST -> 400;
                default -> 500;
            };
            return error(response, status, code, null);
        } catch (UserNotFoundException ex) {
            return error(response, 404, SaayamStatusCode.USER_NOT_FOUND, new Object[]{ex.getUserId()});
        } catch (ResponseStatusException ex) {
            int status = ex.getStatusCode().value();
            SaayamStatusCode code = status == 401 ? SaayamStatusCode.UNAUTHORIZED : SaayamStatusCode.FORBIDDEN;
            return error(response, status, code, null);
        } catch (IllegalArgumentException ex) {
            return error(response, 400, SaayamStatusCode.BAD_REQUEST, null);
        } catch (Exception ex) {
            if (context != null) context.getLogger().log("Notification action failed: " + ex.getClass().getSimpleName());
            return error(response, 500, SaayamStatusCode.INTERNAL_SERVER_ERROR, null);
        }
    }

    private static Integer notificationId(APIGatewayProxyRequestEvent event) {
        String value = event.getPathParameters() == null ? null : event.getPathParameters().get("notificationId");
        if (value == null) throw new IllegalArgumentException("Missing notificationId");
        int id = Integer.parseInt(value);
        if (id <= 0) throw new IllegalArgumentException("Invalid notificationId");
        return id;
    }

    private static String header(APIGatewayProxyRequestEvent event, String name) {
        if (event.getHeaders() == null) return null;
        return event.getHeaders().entrySet().stream().filter(e -> name.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private static String callerEmail(APIGatewayProxyRequestEvent event) {
        var authorizer = event.getRequestContext() == null ? null : event.getRequestContext().getAuthorizer();
        if (authorizer == null) return null;
        Object claims = authorizer.get("claims");
        return claims instanceof Map<?, ?> map && map.get("email") instanceof String email ? email : null;
    }

    private APIGatewayProxyResponseEvent error(APIGatewayProxyResponseEvent response, int status,
                                               SaayamStatusCode code, Object[] args) {
        try {
            String message = messages.getMessage(code.getCode(), args);
            return response.withStatusCode(status).withBody(JSON.writeValueAsString(
                    responses.buildErrorResponse(status, code, message)));
        } catch (RuntimeException | JsonProcessingException ex) {
            return response.withStatusCode(500).withBody(
                    "{\"success\":false,\"statusCode\":500,\"saayamCode\":\"SAAAYAM-1350\",\"message\":\"Internal server error\"}");
        }
    }
}
