package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.NotificationActionResponse;
import org.sfa.volunteer.entities.Notifications;
import org.sfa.volunteer.enums.ActionStatus;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.repository.NotificationsRepository;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceDenyTest {

    @Mock
    private NotificationsRepository notificationsRepo;

    @Mock
    private UserNotificationStatusRepository userNtfStatusRepo;

    @InjectMocks
    private NotificationServiceImpl service;

    private Notifications pendingNotification;

    @BeforeEach
    void setUp() {
        pendingNotification = new Notifications();
        pendingNotification.setNotificationId(1);
        pendingNotification.setUserId("user-1");
        pendingNotification.setActionStatus(ActionStatus.PENDING);
    }

    @Test
    void denyNotification_success_whenPending() {
        when(notificationsRepo.findByNotificationIdAndUserId(1, "user-1"))
                .thenReturn(Optional.of(pendingNotification));
        when(notificationsRepo.updateActionStatusConditionally(
                1, "user-1", ActionStatus.PENDING, ActionStatus.DENIED))
                .thenReturn(1);

        NotificationActionResponse response = service.denyNotification(1, "user-1");

        assertThat(response.notificationId()).isEqualTo(1);
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.actionStatus()).isEqualTo(ActionStatus.DENIED);
    }

    @Test
    void denyNotification_throwsAlreadyActioned_whenActionStatusIsNull() {
        pendingNotification.setActionStatus(null);
        when(notificationsRepo.findByNotificationIdAndUserId(1, "user-1"))
                .thenReturn(Optional.of(pendingNotification));

        NotificationException ex = assertThrows(NotificationException.class,
                () -> service.denyNotification(1, "user-1"));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.toString());
        verify(notificationsRepo, never()).updateActionStatusConditionally(any(), any(), any(), any());
    }

    @Test
    void denyNotification_throwsNotFound_whenNotificationDoesNotExist() {
        when(notificationsRepo.findByNotificationIdAndUserId(99, "user-1"))
                .thenReturn(Optional.empty());

        NotificationException ex = assertThrows(NotificationException.class,
                () -> service.denyNotification(99, "user-1"));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.NOTIFICATION_NOT_FOUND.toString());
    }

    @Test
    void denyNotification_throwsAlreadyActioned_whenAlreadyAccepted() {
        pendingNotification.setActionStatus(ActionStatus.ACCEPTED);
        when(notificationsRepo.findByNotificationIdAndUserId(1, "user-1"))
                .thenReturn(Optional.of(pendingNotification));

        NotificationException ex = assertThrows(NotificationException.class,
                () -> service.denyNotification(1, "user-1"));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.toString());
        verify(notificationsRepo, never()).updateActionStatusConditionally(any(), any(), any(), any());
    }

    @Test
    void denyNotification_throwsAlreadyActioned_whenAlreadyDenied() {
        pendingNotification.setActionStatus(ActionStatus.DENIED);
        when(notificationsRepo.findByNotificationIdAndUserId(1, "user-1"))
                .thenReturn(Optional.of(pendingNotification));

        NotificationException ex = assertThrows(NotificationException.class,
                () -> service.denyNotification(1, "user-1"));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.toString());
    }

    @Test
    void denyNotification_throwsAlreadyActioned_whenConcurrentUpdateWins() {
        when(notificationsRepo.findByNotificationIdAndUserId(1, "user-1"))
                .thenReturn(Optional.of(pendingNotification));
        when(notificationsRepo.updateActionStatusConditionally(
                1, "user-1", ActionStatus.PENDING, ActionStatus.DENIED))
                .thenReturn(0);

        NotificationException ex = assertThrows(NotificationException.class,
                () -> service.denyNotification(1, "user-1"));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.toString());
    }

    @Test
    void denyNotification_throwsBadRequest_whenUserIdBlank() {
        NotificationException ex = assertThrows(NotificationException.class,
                () -> service.denyNotification(1, "  "));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.BAD_REQUEST.toString());
        verifyNoInteractions(notificationsRepo);
    }

    @Test
    void denyNotification_throwsBadRequest_whenNotificationIdNull() {
        NotificationException ex = assertThrows(NotificationException.class,
                () -> service.denyNotification(null, "user-1"));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.BAD_REQUEST.toString());
        verifyNoInteractions(notificationsRepo);
    }
}
