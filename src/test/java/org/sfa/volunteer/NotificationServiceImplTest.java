package org.sfa.volunteer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.NotificationResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;
import org.sfa.volunteer.enums.StatusType;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.repository.NotificationsRepository;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.impl.NotificationServiceImpl;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceImplTest {

    @Mock
    private NotificationsRepository nRepository;

    @Mock
    private UserNotificationStatusRepository userNSRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetNotificationCounts_whenWatermarkExists() {

        String userId = "user123";
        GetNotificationsRequest request = new GetNotificationsRequest(userId, 0, 0);

        Timestamp watermark = Timestamp.from(Instant.now());

        when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(watermark);
        when(nRepository.countAllNotifications(userId)).thenReturn(10);
        when(nRepository.countNewNotifications(userId, watermark)).thenReturn(3);

        GetNotificationsResponse response = notificationService.getNotificationCounts(request);

        assertNotNull(response);
        assertEquals(10, response.totalCount());
        assertEquals(3, response.newNotificationsCount());
        assertNull(response.notifications());
    }

    @Test
    void testGetNotificationCounts_whenWatermarkIsNull() {

        String userId = "user123";
        GetNotificationsRequest request = new GetNotificationsRequest(userId, 0, 0);

        when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(null);
        when(nRepository.countAllNotifications(userId)).thenReturn(5);
        when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(5);

        GetNotificationsResponse response = notificationService.getNotificationCounts(request);

        assertNotNull(response);
        assertEquals(5, response.totalCount());
        assertEquals(5, response.newNotificationsCount());
        assertNull(response.notifications());
    }

    // GetNotifications Test Cases
    @Test
    void testGetNotifications_whenWatermarkExists() {

        String userId = "U1";
        int rowStart = 0;
        int rowEnd = 9;

        GetNotificationsRequest request = new GetNotificationsRequest(userId, rowStart, rowEnd);

        Timestamp watermark = Timestamp.from(Instant.now());

        NotificationResponse mockNotification = NotificationResponse.builder()
                .notificationId(1)
                .status("ignored")
                .typeName("Help")
                .message("Old message")
                .createDttm(Timestamp.from(Instant.now().minusSeconds(7200))) // 2 hours ago
                .build();

        when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(watermark);
        when(nRepository.findNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(mockNotification));
        when(nRepository.countAllNotifications(userId)).thenReturn(10);
        when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(3);

        GetNotificationsResponse response = notificationService.getNotifications(request);

        assertNotNull(response);
        assertEquals(10, response.totalCount());
        assertEquals(3, response.newNotificationsCount());
        assertEquals(1, response.notifications().size());
        assertEquals("Help", response.notifications().get(0).typeName());

        assertEquals("old", response.notifications().get(0).status());
    }

    @Test
    void testGetNotifications_whenWatermarkExists_new() {

        String userId = "U1";
        int rowStart = 0;
        int rowEnd = 9;

        GetNotificationsRequest request = new GetNotificationsRequest(userId, rowStart, rowEnd);

        Timestamp watermark = Timestamp.from(Instant.now());

        NotificationResponse mockNotification = NotificationResponse.builder()
                .notificationId(1)
                .status("ignored")
                .typeName("Help")
                .message("Old message")
                .createDttm(Timestamp.from(Instant.now().plusSeconds(100)))
                .build();

        when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(watermark);
        when(nRepository.findNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(mockNotification));
        when(nRepository.countAllNotifications(userId)).thenReturn(10);
        when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(3);

        GetNotificationsResponse response = notificationService.getNotifications(request);

        assertNotNull(response);
        assertEquals(10, response.totalCount());
        assertEquals(3, response.newNotificationsCount());
        assertEquals(1, response.notifications().size());
        assertEquals("Help", response.notifications().get(0).typeName());

        assertEquals("new", response.notifications().get(0).status());
    }

    @Test
    void testGetNotifications_whenWatermarkIsNull() {

        String userId = "U1";
        int rowStart = 0;
        int rowEnd = 4;

        GetNotificationsRequest request = new GetNotificationsRequest(userId, rowStart, rowEnd);

        NotificationResponse mockNotification = NotificationResponse.builder()
                .notificationId(1)
                .status("ignored")
                .typeName("Alert")
                .message("Test")
                .createDttm(Timestamp.from(Instant.now().minusSeconds(5000)))
                .build();

        when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(null);
        when(nRepository.findNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(mockNotification));
        when(nRepository.countAllNotifications(userId)).thenReturn(5);
        when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(5);

        GetNotificationsResponse response = notificationService.getNotifications(request);

        assertNotNull(response);
        assertEquals(5, response.totalCount());
        assertEquals(5, response.newNotificationsCount());
        assertEquals(1, response.notifications().size());
        assertEquals("Alert", response.notifications().get(0).typeName());
        // A user with no watermark row has never opened the notifications page, so
        // every notification is new to them. This previously asserted "old", which
        // contradicted the newNotificationsCount() of 5 asserted just above.
        assertEquals("new", response.notifications().get(0).status());
    }

    @Test
    void getNotifications_shouldHandleEmptyList() {

        String userId = "U1";
        GetNotificationsRequest req = new GetNotificationsRequest(userId, 0, 10);

        when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(Timestamp.from(Instant.now()));
        when(nRepository.findNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(List.of());
        when(nRepository.countAllNotifications(userId)).thenReturn(0);
        when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(0);

        GetNotificationsResponse response = notificationService.getNotifications(req);

        assertEquals(0, response.totalCount());
        assertEquals(0, response.newNotificationsCount());
        assertTrue(response.notifications().isEmpty());
    }

    // UpdateLastSeen TestCases
    // -------------------------------
    // 1. userId is null → throw exception
    // -------------------------------
    @Test
    void testUpdateLastSeen_whenUserIdIsNull() {
        UpsertLastSeenRequest request = new UpsertLastSeenRequest(null);

        assertThrows(UserNotFoundException.class,
                () -> notificationService.upsertLastSeen(request));
    }

    // -------------------------------
    // 2. userId is blank → throw exception
    // -------------------------------
    @Test
    void testUpdateLastSeen_whenUserIdIsBlank() {
        UpsertLastSeenRequest request = new UpsertLastSeenRequest("   ");

        assertThrows(UserNotFoundException.class,
                () -> notificationService.upsertLastSeen(request));
    }

    @Test
    void upsertLastSeen_shouldCreateRecord_whenRecordDoesNotExist() {
        String userId = "U1";

        UpsertLastSeenRequest req = UpsertLastSeenRequest.builder()
                .userId(userId)
                .build();

        when(userNSRepository.existsByUserId(userId)).thenReturn(0);
        when(userNSRepository.createLastSeenTimestamp(eq(userId), any())).thenReturn(1);

        UpsertLastSeenResponse res = notificationService.upsertLastSeen(req);

        assertTrue(res.created());
        assertFalse(res.updated());
        assertEquals("Created", res.message());
    }

    // -------------------------------
    // 4. userId exists → update succeeds
    // -------------------------------
    @Test
    void testUpdateLastSeen_whenUpdateSucceeds() {
        String userId = "U1";
        UpsertLastSeenRequest request = new UpsertLastSeenRequest(userId);

        when(userNSRepository.existsByUserId(userId)).thenReturn(1);
        when(userNSRepository.updateLastSeenTimestamp(eq(userId), any(Timestamp.class)))
                .thenReturn(1); // update success

        UpsertLastSeenResponse response = notificationService.upsertLastSeen(request);

        assertTrue(response.updated());
        assertEquals("Updated", response.message());
    }

    // -------------------------------
    // 5. userId exists → update fails
    // -------------------------------
    @Test
    void testUpdateLastSeen_whenUpdateFails() {
        String userId = "U1";
        UpsertLastSeenRequest request = new UpsertLastSeenRequest(userId);

        when(userNSRepository.existsByUserId(userId)).thenReturn(1);
        when(userNSRepository.updateLastSeenTimestamp(eq(userId), any(Timestamp.class)))
                .thenReturn(0); // update failed

        UpsertLastSeenResponse response = notificationService.upsertLastSeen(request);

        assertFalse(response.updated());
        assertEquals("failed", response.message());
    }

    @Test
    void upsertLastSeen_shouldThrowException_whenDatabaseFails() {
        String userId = "U1";

        UpsertLastSeenRequest req = UpsertLastSeenRequest.builder()
                .userId(userId)
                .build();

        when(userNSRepository.existsByUserId(userId)).thenReturn(1);
        when(userNSRepository.updateLastSeenTimestamp(eq(userId), any()))
                .thenThrow(new DataAccessException("DB error") {
                });

        assertThrows(RuntimeException.class, () -> notificationService.upsertLastSeen(req));
    }
}