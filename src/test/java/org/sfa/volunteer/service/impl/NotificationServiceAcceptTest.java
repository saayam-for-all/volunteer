package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.entities.Notifications;
import org.sfa.volunteer.enums.ActionStatus;
import org.sfa.volunteer.exception.NotificationAcceptException;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.repository.NotificationsRepository;
import org.sfa.volunteer.repository.NotificationsRepository.RequestNotificationState;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceAcceptTest {
    @Mock private NotificationsRepository repository;
    @Mock private UserNotificationStatusRepository watermarkRepository;
    @InjectMocks private NotificationServiceImpl service;
    private Notifications notification;

    @BeforeEach
    void setUp() {
        notification = new Notifications();
        notification.setNotificationId(1);
        notification.setUserId("user-1");
        notification.setRequestId("req-1");
        notification.setActionStatus(ActionStatus.PENDING);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void invalidNotificationIdsNeverReachDatabase(Integer id) {
        assertThatThrownBy(() -> service.acceptNotification(id, "user-1"))
                .isInstanceOfSatisfying(NotificationAcceptException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void invalidCallerIdsNeverReachDatabase(String user) {
        assertThatThrownBy(() -> service.acceptNotification(1, user))
                .isInstanceOfSatisfying(NotificationAcceptException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void notificationWithoutRequestReferenceCannotBeClaimed(String requestId) {
        notification.setRequestId(requestId);
        when(repository.findByNotificationIdAndUserId(1, "user-1")).thenReturn(Optional.of(notification));
        assertThatThrownBy(() -> service.acceptNotification(1, "user-1"))
                .isInstanceOfSatisfying(NotificationAcceptException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).lockByRequestId(any());
        verify(repository, never()).updateActionStatusConditionally(any(), any(), any(), any());
    }

    @Test
    void absentOrUnownedNotificationReturnsNotFound() {
        when(repository.findByNotificationIdAndUserId(1, "user-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.acceptNotification(1, "user-1"))
                .isInstanceOfSatisfying(NotificationAcceptException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(repository, never()).lockByRequestId(any());
    }

    @Test
    void notificationRemovedBeforeLockReturnsNotFound() {
        when(repository.findByNotificationIdAndUserId(1, "user-1")).thenReturn(Optional.of(notification));
        when(repository.lockByRequestId("req-1")).thenReturn(List.of());
        assertThatThrownBy(() -> service.acceptNotification(1, "user-1"))
                .isInstanceOfSatisfying(NotificationAcceptException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(repository, never()).updateActionStatusConditionally(any(), any(), any(), any());
    }

    @Test
    void lockedDeniedStateOverridesStalePendingEntity() {
        arrangeLockedState("DENIED");
        assertThatThrownBy(() -> service.acceptNotification(1, "user-1"))
                .isInstanceOfSatisfying(NotificationAcceptException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).updateActionStatusConditionally(any(), any(), any(), any());
    }

    @Test
    void lostConditionalUpdateDoesNotInvalidateOtherVolunteers() {
        arrangeLockedState("PENDING");
        when(repository.updateActionStatusConditionally(1, "user-1", ActionStatus.PENDING, ActionStatus.ACCEPTED))
                .thenReturn(0);
        assertThatThrownBy(() -> service.acceptNotification(1, "user-1"))
                .isInstanceOfSatisfying(NotificationAcceptException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).markOtherPendingNotificationsTaken(any(), any(), any(), any());
    }

    @Test
    void takenStateCannotBeReopenedEvenIfWinnerNotificationIsNoLongerPresent() {
        arrangeLockedState("REQUEST_TAKEN");
        assertThat(service.acceptNotification(1, "user-1").actionStatus()).isEqualTo(ActionStatus.REQUEST_TAKEN);
        verify(repository, never()).updateActionStatusConditionally(any(), any(), any(), any());
    }

    @Test
    void unexpectedNotificationReadFailureUsesExistingApplicationError() {
        when(repository.countAllNotifications("user-1")).thenThrow(new IllegalStateException("read failed"));
        assertThatThrownBy(() -> service.getNotificationCounts(
                GetNotificationsRequest.builder().userId("user-1").build()))
                .isInstanceOf(NotificationException.class).hasMessage("UNEXPECTED_ERROR");
    }

    private void arrangeLockedState(String status) {
        when(repository.findByNotificationIdAndUserId(1, "user-1")).thenReturn(Optional.of(notification));
        when(repository.lockByRequestId("req-1")).thenReturn(List.of(new LockedState(1, "user-1", status)));
    }

    private record LockedState(Integer id, String user, String action) implements RequestNotificationState {
        public Integer getNotificationId() { return id; }
        public String getUserId() { return user; }
        public String getActionStatus() { return action; }
    }
}
