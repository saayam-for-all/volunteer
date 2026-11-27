package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.NotificationActionResponse;
import org.sfa.volunteer.enums.ActionStatus;
import org.sfa.volunteer.exception.NotificationAcceptException;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificationActionHandlersTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private NotificationService notifications;
    private UserService users;
    private AcceptNotificationHandler accept;
    private DenyNotificationHandler deny;

    @BeforeEach
    void setUp() {
        notifications = mock(NotificationService.class);
        users = mock(UserService.class);
        var source = new ResourceBundleMessageSource();
        source.setBasename("lang/messages");
        var messages = new MessageSourceUtil(source);
        var responses = new ResponseBuilder(messages);
        accept = new AcceptNotificationHandler(notifications, users, responses, messages);
        deny = new DenyNotificationHandler(notifications, users, responses, messages);
        when(users.userExists("user-1")).thenReturn(true);
        when(users.isEmailOwnedByUser("user-1", "caller@example.test")).thenReturn(true);
    }

    private APIGatewayProxyRequestEvent event() {
        return new APIGatewayProxyRequestEvent().withHttpMethod("POST")
                .withPathParameters(Map.of("notificationId", "19801"))
                .withHeaders(Map.of("x-user-id", "user-1"))
                .withRequestContext(authorizer("caller@example.test"));
    }

    private static APIGatewayProxyRequestEvent.ProxyRequestContext authorizer(String email) {
        var context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(Map.of("claims", Map.of("email", email)));
        return context;
    }

    @Test
    void acceptUsesSharedServiceAndPreservesResponseContract() throws Exception {
        when(notifications.acceptNotification(19801, "user-1"))
                .thenReturn(new NotificationActionResponse(19801, "user-1", ActionStatus.ACCEPTED, "req-1"));
        var response = accept.handleRequest(event(), null);
        assertThat(response.getStatusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.at("/data/actionStatus").asText()).isEqualTo("ACCEPTED");
        assertThat(body.at("/data/requestId").asText()).isEqualTo("req-1");
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("timestamp").isTextual()).isTrue();
        verify(users).isEmailOwnedByUser("user-1", "caller@example.test");
        verify(notifications, never()).denyNotification(any(), any());
    }

    @Test
    void denyUsesExistingDenyService() throws Exception {
        when(notifications.denyNotification(19801, "user-1"))
                .thenReturn(new NotificationActionResponse(19801, "user-1", ActionStatus.DENIED));
        var response = deny.handleRequest(event(), null);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(JSON.readTree(response.getBody()).at("/data/actionStatus").asText()).isEqualTo("DENIED");
        verify(notifications, never()).acceptNotification(any(), any());
    }

    @Test
    void losingAcceptReturnsConflictAndTakenStatus() throws Exception {
        when(notifications.acceptNotification(19801, "user-1"))
                .thenReturn(new NotificationActionResponse(19801, "user-1", ActionStatus.REQUEST_TAKEN, "req-1"));
        var response = accept.handleRequest(event(), null);
        assertThat(response.getStatusCode()).isEqualTo(409);
        var body = JSON.readTree(response.getBody());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.at("/data/actionStatus").asText()).isEqualTo("REQUEST_TAKEN");
        assertThat(body.path("saayamCode").asText()).isEqualTo("SAAAYAM-1323");
    }

    @Test
    void bothHandlersAllowUnauthenticatedPreflightWithoutServiceCalls() {
        for (var handler : new NotificationActionHandlerBase[]{accept, deny}) {
            var response = handler.handleRequest(new APIGatewayProxyRequestEvent().withHttpMethod("OPTIONS"), null);
            assertThat(response.getStatusCode()).isEqualTo(204);
            assertThat(response.getHeaders()).containsEntry("Content-Type", "application/json");
            assertThat(response.getHeaders().get("Access-Control-Allow-Headers")).contains("Authorization", "X-User-Id");
        }
        verifyNoInteractions(notifications, users);
    }

    @Test
    void suppliedHeaderOrBodyCannotReplaceTrustedGatewayClaims() {
        var request = event().withRequestContext(null)
                .withBody("{\"userId\":\"user-1\",\"email\":\"caller@example.test\"}")
                .withHeaders(Map.of("X-User-Id", "user-1", "Authorization", "Bearer unsigned-token"));
        assertThat(accept.handleRequest(request, null).getStatusCode()).isEqualTo(401);
        assertThat(deny.handleRequest(request, null).getStatusCode()).isEqualTo(401);
        assertThat(accept.handleRequest(event().withRequestContext(
                new APIGatewayProxyRequestEvent.ProxyRequestContext()), null).getStatusCode()).isEqualTo(401);
        assertThat(accept.handleRequest(event().withRequestContext(authorizer(" ")), null).getStatusCode()).isEqualTo(401);
        verifyNoInteractions(notifications, users);
    }

    @Test
    void differentUsersIdentityIsForbidden() {
        when(users.isEmailOwnedByUser("user-1", "caller@example.test")).thenReturn(false);
        assertThat(accept.handleRequest(event(), null).getStatusCode()).isEqualTo(403);
        verifyNoInteractions(notifications);
    }

    @Test
    void nonexistentUserIsNotFound() {
        when(users.userExists("user-1")).thenReturn(false);
        assertThat(accept.handleRequest(event(), null).getStatusCode()).isEqualTo(404);
        verifyNoInteractions(notifications);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "0", "-1", "2147483648"})
    void invalidPathIdsNeverReachTheService(String id) {
        var request = event().withPathParameters(id == null ? null : Map.of("notificationId", id));
        assertThat(accept.handleRequest(request, null).getStatusCode()).isEqualTo(400);
        verifyNoInteractions(notifications, users);
    }

    @Test
    void missingOrBlankUserHeaderIsBadRequest() {
        assertThat(accept.handleRequest(event().withHeaders(null), null).getStatusCode()).isEqualTo(400);
        assertThat(accept.handleRequest(event().withHeaders(Map.of("X-User-Id", " ")), null).getStatusCode()).isEqualTo(400);
        verifyNoInteractions(notifications, users);
    }

    @Test
    void nullEventAndUnsupportedMethodsAreRejected() {
        assertThat(accept.handleRequest(null, null).getStatusCode()).isEqualTo(400);
        var response = accept.handleRequest(event().withHttpMethod("GET"), null);
        assertThat(response.getStatusCode()).isEqualTo(405);
        assertThat(response.getHeaders()).containsEntry("Allow", "POST,OPTIONS");
        assertThat(deny.handleRequest(event().withHttpMethod(null), null).getStatusCode()).isEqualTo(405);
        verifyNoInteractions(notifications, users);
    }

    @Test
    void typedAcceptFailureKeepsItsStatus() throws Exception {
        when(notifications.acceptNotification(19801, "user-1"))
                .thenThrow(new NotificationAcceptException(SaayamStatusCode.NOTIFICATION_NOT_FOUND,
                        HttpStatus.NOT_FOUND, "user-1", 19801));
        var response = accept.handleRequest(event(), null);
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.getBody()).path("saayamCode").asText()).isEqualTo("SAAAYAM-1321");
    }

    @ParameterizedTest
    @CsvSource({"NOTIFICATION_NOT_FOUND,404", "NOTIFICATION_ALREADY_ACTIONED,409", "USER_NOT_FOUND,404",
            "BAD_REQUEST,400", "INVALID_PARAMETER,400", "DATABASE_ERROR,500", "UNEXPECTED_ERROR,500"})
    void denyDomainErrorsReturnActualHttpStatus(String error, int status) {
        when(notifications.denyNotification(19801, "user-1")).thenThrow(new NotificationException(error, "user-1"));
        assertThat(deny.handleRequest(event(), null).getStatusCode()).isEqualTo(status);
    }

    @Test
    void internalFailuresDoNotExposeExceptionMessages() throws Exception {
        when(notifications.acceptNotification(19801, "user-1")).thenThrow(new IllegalStateException("private details"));
        var context = mock(Context.class);
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
        var response = accept.handleRequest(event(), context);
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).doesNotContain("private details");
        assertThat(JSON.readTree(response.getBody()).path("success").asBoolean()).isFalse();
    }

    @Test
    void authorizationFailureKeepsUnauthorizedStatus() {
        when(users.isEmailOwnedByUser("user-1", "caller@example.test"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        assertThat(accept.handleRequest(event(), null).getStatusCode()).isEqualTo(401);
    }

    @Test
    void messageLookupFailureStillProducesJsonServerError() throws Exception {
        var messages = mock(MessageSourceUtil.class);
        when(messages.getMessage(anyString(), any())).thenThrow(new IllegalStateException("unavailable"));
        var handler = new AcceptNotificationHandler(notifications, users, new ResponseBuilder(messages), messages);
        var response = handler.handleRequest(null, null);
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(JSON.readTree(response.getBody()).path("success").asBoolean()).isFalse();
    }
}
