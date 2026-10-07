package org.sfa.volunteer.serviceTest;

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
import org.sfa.volunteer.entities.UserNotificationStatus;
import org.sfa.volunteer.enums.StatusType;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.repository.NotificationsRepository;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.impl.NotificationServiceImpl;
import org.springframework.dao.DataAccessException;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

                // CRUD: Mock DB entity (READ)
                UserNotificationStatus status = new UserNotificationStatus();
                status.setUserId(userId);
                status.setWatermarkTimestamp(Timestamp.from(Instant.now().minusSeconds(100)));

                when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(status.getWatermarkTimestamp());
                when(nRepository.countAllNotifications(userId)).thenReturn(10);
                when(nRepository.countNewNotifications(userId, status.getWatermarkTimestamp())).thenReturn(3);

                GetNotificationsResponse response = notificationService.getNotificationCounts(request);

                assertNotNull(response);
                assertEquals(10, response.totalCount());
                assertEquals(3, response.newNotificationsCount());
                assertNull(response.notifications());

                // Verify CRUD interactions
                verify(userNSRepository, times(1)).getLastSeenTimestamp(userId);
                verify(nRepository, times(1)).countAllNotifications(userId);
                verify(nRepository, times(1))
                                .countNewNotifications(userId, status.getWatermarkTimestamp());
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

                // Verify CRUD interactions
                verify(userNSRepository, times(1)).getLastSeenTimestamp(userId);
                verify(nRepository, times(1)).countAllNotifications(userId);
                verify(nRepository, never())
                                .countNewNotifications(eq(userId), any(Timestamp.class));
        }

        // GetNotifications Test Cases
        @Test
        void testGetNotifications_whenWatermarkExists() {

                String userId = "U1";
                GetNotificationsRequest request = new GetNotificationsRequest(userId, 0, 9);

                Timestamp watermark = Timestamp.from(Instant.now());

                // Mock DB row (Object[])
                Object[] row = new Object[] {
                                1L, // notification_id
                                "ignored", // status (ignored by service)
                                "Help", // type_name
                                "Old message", // message
                                Timestamp.from(Instant.now().minusSeconds(7200)) // older than watermark
                };

                // Page<Object[]> mock
                List<Object[]> rows = Arrays.<Object[]>asList(row);
                Page<Object[]> mockPage = new PageImpl<Object[]>(rows);

                // Mock repository calls
                when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(watermark);
                when(nRepository.findNotifications(eq(userId), any(Pageable.class))).thenReturn(mockPage);
                when(nRepository.countAllNotifications(userId)).thenReturn(10);
                when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(3);

                // Execute service
                GetNotificationsResponse response = notificationService.getNotifications(request);

                // Assertions
                assertNotNull(response);
                assertEquals(10, response.totalCount());
                assertEquals(3, response.newNotificationsCount());
                assertEquals(1, response.notifications().size());
                assertEquals("Help", response.notifications().get(0).typeName());
                assertEquals("old", response.notifications().get(0).status());

                // Verify CRUD interactions
                verify(userNSRepository).getLastSeenTimestamp(userId);
                verify(nRepository).findNotifications(eq(userId), any(Pageable.class));
                verify(nRepository).countAllNotifications(userId);
                verify(nRepository).countNewNotifications(eq(userId), eq(watermark));
        }

        @Test
        void testGetNotifications_whenWatermarkExists_new() {

                String userId = "U1";
                GetNotificationsRequest request = new GetNotificationsRequest(userId, 0, 9);

                Timestamp watermark = Timestamp.from(Instant.now());

                // Mock DB row (Object[])
                Object[] row = new Object[] {
                                1L, // notification_id
                                "ignored", // status (ignored by service)
                                "Help", // type_name
                                "Old message", // message
                                Timestamp.from(Instant.now().plusSeconds(100)) // newer than watermark → NEW
                };

                // Page<Object[]> mock
                List<Object[]> rows = Arrays.<Object[]>asList(row);
                Page<Object[]> mockPage = new PageImpl<Object[]>(rows);

                // Repository mocks
                when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(watermark);
                when(nRepository.findNotifications(eq(userId), any(Pageable.class))).thenReturn(mockPage);
                when(nRepository.countAllNotifications(userId)).thenReturn(10);
                when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(3);

                // Execute service
                GetNotificationsResponse response = notificationService.getNotifications(request);

                // Assertions
                assertNotNull(response);
                assertEquals(10, response.totalCount());
                assertEquals(3, response.newNotificationsCount());
                assertEquals(1, response.notifications().size());
                assertEquals("Help", response.notifications().get(0).typeName());
                assertEquals("new", response.notifications().get(0).status());

                // CRUD verification
                verify(userNSRepository, times(1)).getLastSeenTimestamp(userId);
                verify(nRepository, times(1)).findNotifications(eq(userId), any(Pageable.class));
                verify(nRepository, times(1)).countAllNotifications(userId);
                verify(nRepository, times(1))
                                .countNewNotifications(eq(userId), eq(watermark));
        }

        @Test
        void testGetNotifications_whenWatermarkIsNull() {

                String userId = "U1";
                int rowStart = 0;
                int rowEnd = 4;

                GetNotificationsRequest request = new GetNotificationsRequest(userId, rowStart, rowEnd);

                // Mock DB row (Object[])
                Object[] row = new Object[] {
                                1L, // notification_id
                                "ignored", // status (ignored by service)
                                "Alert", // type_name
                                "old", // message
                                Timestamp.from(Instant.now().plusSeconds(100)) // NEW (after watermark)
                };

                List<Object[]> rows = Arrays.<Object[]>asList(row);
                Page<Object[]> mockPage = new PageImpl<Object[]>(rows);

                when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(null);
                when(nRepository.findNotifications(eq(userId), any(Pageable.class)))
                                .thenReturn(mockPage);
                when(nRepository.countAllNotifications(userId)).thenReturn(5);
                when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(5);

                GetNotificationsResponse response = notificationService.getNotifications(request);

                assertNotNull(response);
                assertEquals(5, response.totalCount());
                assertEquals(5, response.newNotificationsCount());
                assertEquals(1, response.notifications().size());
                assertEquals("Alert", response.notifications().get(0).typeName());
                // A user with no watermark row has never opened the notifications page,
                // so every notification is new to them -- consistent with the
                // newNotificationsCount() of 5 asserted just above. Note this row is
                // stamped in the future, so it reads "new" under any watermark default;
                // testGetNotifications_whenWatermarkIsNull_andNotificationIsOld covers the
                // case that actually discriminates the two.
                assertEquals("new", response.notifications().get(0).status());

                // CRUD verification
                verify(userNSRepository, times(1)).getLastSeenTimestamp(userId);
                verify(nRepository, times(1)).findNotifications(eq(userId), any(Pageable.class));
                verify(nRepository, times(1)).countAllNotifications(userId);
                verify(nRepository, times(1))
                                .countNewNotifications(eq(userId), any(Timestamp.class));
        }

        /**
         * Regression: a user who has never opened the notification centre has no
         * watermark row. Defaulting that watermark to "now" marks every existing
         * notification "old", even though getNotificationCounts() reports them all as
         * new. The row here is stamped in the PAST, so it only reads "new" if the
         * missing watermark is treated as "the beginning of time".
         */
        @Test
        void testGetNotifications_whenWatermarkIsNull_andNotificationIsOld() {

                String userId = "U1";
                GetNotificationsRequest request = new GetNotificationsRequest(userId, 0, 4);

                Object[] row = new Object[] {
                                1L, // notification_id
                                "ignored", // status (recomputed by the service)
                                "Alert", // type_name
                                "an hour ago", // message
                                Timestamp.from(Instant.now().minusSeconds(3600)) // created in the PAST
                };

                Page<Object[]> mockPage = new PageImpl<Object[]>(Arrays.<Object[]>asList(row));

                when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(null);
                when(nRepository.findNotifications(eq(userId), any(Pageable.class)))
                                .thenReturn(mockPage);
                when(nRepository.countAllNotifications(userId)).thenReturn(1);
                when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(1);

                GetNotificationsResponse response = notificationService.getNotifications(request);

                assertEquals(1, response.notifications().size());
                assertEquals("new", response.notifications().get(0).status(),
                                "a never-seen user's existing notifications must read as new");
                assertEquals(1, response.newNotificationsCount());
        }

        /**
         * Regression: rowStart/rowEnd are absolute row offsets, not a page index.
         * PageRequest.of(rowStart / limit, limit) only lands on the requested row when
         * rowStart is an exact multiple of the window size; rowStart=3/rowEnd=7 gives
         * limit=5 and page 0, silently returning rows 0-4 instead of 3-7.
         */
        @Test
        void testGetNotifications_usesAbsoluteRowOffset() {

                String userId = "U1";
                GetNotificationsRequest request = new GetNotificationsRequest(userId, 3, 7);

                when(userNSRepository.getLastSeenTimestamp(userId))
                                .thenReturn(Timestamp.from(Instant.now()));
                when(nRepository.findNotifications(eq(userId), any(Pageable.class)))
                                .thenReturn(new PageImpl<Object[]>(List.of()));
                when(nRepository.countAllNotifications(userId)).thenReturn(20);
                when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(0);

                notificationService.getNotifications(request);

                ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
                verify(nRepository).findNotifications(eq(userId), pageable.capture());

                assertEquals(3L, pageable.getValue().getOffset(),
                                "rowStart must be used as an absolute row offset");
                assertEquals(5, pageable.getValue().getPageSize(),
                                "window size must be rowEnd - rowStart + 1");
        }

        /**
         * Regression: the watermark column is TIMESTAMP WITHOUT TIME ZONE and the spec
         * requires GMT. Timestamp.from(Instant.now()) renders in the JVM default zone
         * and PgJDBC writes that rendering verbatim, so on a non-UTC JVM the stored
         * wall-clock is local time. This asserts the stored wall-clock is UTC.
         */
        @Test
        void upsertLastSeen_shouldStoreWatermarkAsUtcWallClock() {

                String userId = "U1";

                when(userNSRepository.existsByUserId(userId)).thenReturn(0);
                when(userNSRepository.createLastSeenTimestamp(eq(userId), any(Timestamp.class)))
                                .thenReturn(1);

                notificationService.upsertLastSeen(new UpsertLastSeenRequest(userId));

                ArgumentCaptor<Timestamp> stored = ArgumentCaptor.forClass(Timestamp.class);
                verify(userNSRepository).createLastSeenTimestamp(eq(userId), stored.capture());

                LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
                long skewSeconds = Math.abs(
                                Duration.between(stored.getValue().toLocalDateTime(), utcNow).getSeconds());

                assertTrue(skewSeconds < 60,
                                "watermark wall-clock must be UTC, was off by " + skewSeconds + "s");
        }

        @Test
        void getNotifications_shouldHandleEmptyList() {

                String userId = "U1";
                GetNotificationsRequest req = new GetNotificationsRequest(userId, 0, 10);

                Timestamp watermark = Timestamp.from(Instant.now());

                // Empty page content
                Page<Object[]> emptyPage = new PageImpl<>(List.of());

                when(userNSRepository.getLastSeenTimestamp(userId)).thenReturn(watermark);
                when(nRepository.findNotifications(eq(userId), any(Pageable.class)))
                                .thenReturn(emptyPage);
                when(nRepository.countAllNotifications(userId)).thenReturn(0);
                when(nRepository.countNewNotifications(eq(userId), any(Timestamp.class))).thenReturn(0);

                GetNotificationsResponse response = notificationService.getNotifications(req);

                assertEquals(0, response.totalCount());
                assertEquals(0, response.newNotificationsCount());
                assertTrue(response.notifications().isEmpty());

                // CRUD verification
                verify(userNSRepository, times(1)).getLastSeenTimestamp(userId);
                verify(nRepository, times(1)).findNotifications(eq(userId), any(Pageable.class));
                verify(nRepository, times(1)).countAllNotifications(userId);
                verify(nRepository, times(1)).countNewNotifications(eq(userId), eq(watermark));
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

                // CRUD verification
                verify(userNSRepository, times(1)).existsByUserId(userId); // READ
                verify(userNSRepository, times(1))
                                .createLastSeenTimestamp(eq(userId), any()); // CREATE
                verify(userNSRepository, never())
                                .updateLastSeenTimestamp(anyString(), any()); // UPDATE should NOT happen
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

                // CRUD verification
                verify(userNSRepository, times(1)).existsByUserId(userId); // READ
                verify(userNSRepository, times(1))
                                .updateLastSeenTimestamp(eq(userId), any(Timestamp.class)); // UPDATE
                verify(userNSRepository, never())
                                .createLastSeenTimestamp(anyString(), any());
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

                // CRUD verification
                verify(userNSRepository, times(1)).existsByUserId(userId); // READ
                verify(userNSRepository, times(1))
                                .updateLastSeenTimestamp(eq(userId), any(Timestamp.class)); // UPDATE attempted
                verify(userNSRepository, never())
                                .createLastSeenTimestamp(anyString(), any());
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

                // CRUD verification
                verify(userNSRepository, times(1)).existsByUserId(userId); // READ
                verify(userNSRepository, times(1))
                                .updateLastSeenTimestamp(eq(userId), any()); // UPDATE attempted
                verify(userNSRepository, never())
                                .createLastSeenTimestamp(anyString(), any());
        }
}