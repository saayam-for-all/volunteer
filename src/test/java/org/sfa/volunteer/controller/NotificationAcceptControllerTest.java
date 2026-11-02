package org.sfa.volunteer.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.NotificationActionResponse;
import org.sfa.volunteer.enums.ActionStatus;
import org.sfa.volunteer.exception.GlobalExceptionHandler;
import org.sfa.volunteer.exception.NotificationAcceptException;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationAcceptControllerTest {
    private NotificationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        var source = new ResourceBundleMessageSource();
        source.setBasename("lang/messages");
        var messages = new MessageSourceUtil(source);
        var responses = new ResponseBuilder(messages);
        mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service, responses))
                .setControllerAdvice(new GlobalExceptionHandler(messages, responses)).build();
    }

    @Test
    void acceptReturnsExistingResponseShapeWithRequestReference() throws Exception {
        when(service.acceptNotification(1, "user-1"))
                .thenReturn(new NotificationActionResponse(1, "user-1", ActionStatus.ACCEPTED, "req-1"));
        mvc.perform(post("/0.0.1/notifications/1/accept").header("X-User-Id", "user-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationId").value(1))
                .andExpect(jsonPath("$.data.requestId").value("req-1"))
                .andExpect(jsonPath("$.data.actionStatus").value("ACCEPTED"));
    }

    @Test
    void takenRequestReturnsActualHttpConflictAndApplicationStatus() throws Exception {
        when(service.acceptNotification(1, "user-1"))
                .thenReturn(new NotificationActionResponse(1, "user-1", ActionStatus.REQUEST_TAKEN, "req-1"));
        mvc.perform(post("/0.0.1/notifications/1/accept").header("X-User-Id", "user-1"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.saayamCode").value(SaayamStatusCode.NOTIFICATION_REQUEST_TAKEN.getCode()))
                .andExpect(jsonPath("$.data.actionStatus").value("REQUEST_TAKEN"));
    }

    @Test
    void missingOrMalformedHeadersAndIdsDoNotCallService() throws Exception {
        mvc.perform(post("/0.0.1/notifications/1/accept")).andExpect(status().isBadRequest());
        mvc.perform(post("/0.0.1/notifications/not-an-id/accept").header("X-User-Id", "user-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void acceptDomainErrorsHaveCorrectHttpStatus() throws Exception {
        for (var status : new HttpStatus[]{HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND, HttpStatus.CONFLICT}) {
            var code = status == HttpStatus.NOT_FOUND ? SaayamStatusCode.NOTIFICATION_NOT_FOUND
                    : status == HttpStatus.CONFLICT ? SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED
                    : SaayamStatusCode.BAD_REQUEST;
            doThrow(new NotificationAcceptException(code, status, "user-1", 1))
                    .when(service).acceptNotification(1, "user-1");
            mvc.perform(post("/0.0.1/notifications/1/accept").header("X-User-Id", "user-1"))
                    .andExpect(status().is(status.value()))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Test
    void denySuccessStillUsesExistingEndpointAndStatus() throws Exception {
        when(service.denyNotification(1, "user-1"))
                .thenReturn(new NotificationActionResponse(1, "user-1", ActionStatus.DENIED));
        mvc.perform(post("/0.0.1/notifications/1/deny").header("X-User-Id", "user-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.actionStatus").value("DENIED"));
    }

    @Test
    void denyAfterAcceptReturnsActualConflict() throws Exception {
        when(service.denyNotification(1, "user-1"))
                .thenThrow(new NotificationException(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.name(), "user-1"));
        mvc.perform(post("/0.0.1/notifications/1/deny").header("X-User-Id", "user-1"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void invalidNotificationPageRangesAreRejectedBeforeCallingService() throws Exception {
        for (String[] range : new String[][]{{"-1", "5"}, {"0", "-1"}, {"5", "1"}}) {
            mvc.perform(get("/0.0.1/notifications/user-1")
                            .param("rowStart", range[0]).param("rowEnd", range[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.saayamCode").value(SaayamStatusCode.INVALID_PARAMETER.getCode()));
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingAndNonnumericPageParametersAreRejectedBySpring() throws Exception {
        mvc.perform(get("/0.0.1/notifications/user-1").param("rowStart", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/0.0.1/notifications/user-1").param("rowEnd", "9"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/0.0.1/notifications/user-1").param("rowStart", "abc").param("rowEnd", "9"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
