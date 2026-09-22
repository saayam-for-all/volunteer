
package org.sfa.volunteer.controllerTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.controller.NotificationController;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;
import org.sfa.volunteer.entities.UserNotificationStatus;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.util.ResponseBuilder;

import jakarta.validation.Valid;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

        @Mock
        private NotificationService notificationService;

        @Mock
        private ResponseBuilder responseBuilder;

        @Mock
        private UserNotificationStatusRepository repository;

        @InjectMocks
        private NotificationController notificationController;

        // ---------------------------------------------------------
        // TEST: GET NOTIFICATION COUNTS
        // ---------------------------------------------------------
        @Test
        void testGetNotificationCounts() throws Exception {

                GetNotificationsRequest request = new GetNotificationsRequest("U123", null, null);

                GetNotificationsResponse mockResponse = new GetNotificationsResponse(0,
                                0, null);

                SaayamResponse<GetNotificationsResponse> mockSaayamResponse = SaayamResponse
                                .<GetNotificationsResponse>builder()
                                .success(true)
                                .statusCode(200)
                                .saayamCode(SaayamStatusCode.SUCCESS.getCode())
                                .message("SUCCESS")
                                .data(mockResponse)
                                .timestamp(ZonedDateTime.now())
                                .build();

                when(notificationService.getNotificationCounts(any(GetNotificationsRequest.class)))
                                .thenReturn(mockResponse);

                when(responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS,
                                mockResponse))
                                .thenReturn(mockSaayamResponse);

                SaayamResponse<GetNotificationsResponse> result = notificationController.getNotificationCounts(request);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals(200, result.statusCode());
                assertEquals(SaayamStatusCode.SUCCESS.getCode(), result.saayamCode());

                // CRUD verification (controller → service)
                verify(notificationService, times(1))
                                .getNotificationCounts(any(GetNotificationsRequest.class));

                // Controller must use ResponseBuilder
                verify(responseBuilder, times(1))
                                .buildSuccessResponse(SaayamStatusCode.SUCCESS, mockResponse);

                verify(notificationService, times(1))
                                .getNotificationCounts(any(GetNotificationsRequest.class));

                verify(responseBuilder, times(1))
                                .buildSuccessResponse(SaayamStatusCode.SUCCESS, mockResponse);

                verify(repository, never()).getLastSeenTimestamp(anyString());
        }

        @Test
        void testGetNotifications() throws Exception {

                int rowStart = 0;
                int rowEnd = 10;

                GetNotificationsRequest request = new GetNotificationsRequest("U123", 0, 10);

                GetNotificationsResponse mockResponse = new GetNotificationsResponse(rowStart,
                                rowEnd, null);

                SaayamResponse<GetNotificationsResponse> mockSaayamResponse = SaayamResponse
                                .<GetNotificationsResponse>builder()
                                .success(true)
                                .statusCode(200)
                                .saayamCode(SaayamStatusCode.SUCCESS.getCode())
                                .message("SUCCESS")
                                .data(mockResponse)
                                .timestamp(ZonedDateTime.now())
                                .build();

                when(notificationService.getNotifications(any(GetNotificationsRequest.class)))
                                .thenReturn(mockResponse);

                when(responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS,
                                mockResponse))
                                .thenReturn(mockSaayamResponse);

                SaayamResponse<GetNotificationsResponse> result = notificationController.getNotifications(request);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals(200, result.statusCode());
                verify(notificationService,
                                times(1)).getNotifications(any(GetNotificationsRequest.class));

                verify(repository, never()).getLastSeenTimestamp(anyString());
                verify(repository, never()).updateLastSeenTimestamp(anyString(), any());
        }

        @Test
        void testUpdateLastSeen_Success() throws Exception {

                UpsertLastSeenRequest request = new UpsertLastSeenRequest("U1");
                UpsertLastSeenResponse mockResponse = new UpsertLastSeenResponse(true, false, "Updated");

                SaayamResponse<UpsertLastSeenResponse> mockSaayamResponse = SaayamResponse
                                .<UpsertLastSeenResponse>builder()
                                .success(true)
                                .statusCode(200)
                                .saayamCode(SaayamStatusCode.SUCCESS.getCode())
                                .message("SUCCESS")
                                .data(mockResponse)
                                .timestamp(ZonedDateTime.now())
                                .build();

                when(notificationService.upsertLastSeen(any(UpsertLastSeenRequest.class)))
                                .thenReturn(mockResponse);

                when(responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS,
                                mockResponse))
                                .thenReturn(mockSaayamResponse);

                SaayamResponse<UpsertLastSeenResponse> result = notificationController.updateLastSeen(request);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals(200, result.statusCode());
                verify(notificationService,
                                times(1)).upsertLastSeen(any(UpsertLastSeenRequest.class));

                verify(repository, never()).getLastSeenTimestamp(anyString());
                verify(repository, never()).updateLastSeenTimestamp(anyString(), any());
        }

        // -------------------------------
        // 1. userId is null → throw exception
        // -------------------------------
        @Test
        void testUpdateLastSeen_whenUserIdIsNull() {
                UpsertLastSeenRequest request = new UpsertLastSeenRequest(null);

                assertThrows(NotificationException.class,
                                () -> notificationController.updateLastSeen(request));

                // CRUD verification — repository must NOT be called at all
                verify(repository, never()).existsByUserId(anyString());
                verify(repository, never()).updateLastSeenTimestamp(anyString(), any());
                verify(repository, never()).createLastSeenTimestamp(anyString(), any());
        }

        @Test
        void testUpdateLastSeen_whenUserIdIsBlank() {
                UpsertLastSeenRequest request = new UpsertLastSeenRequest("   ");

                assertThrows(NotificationException.class,
                                () -> notificationController.updateLastSeen(request));

                // CRUD verification — repository must NOT be called
                verify(repository, never()).existsByUserId(anyString());
                verify(repository, never()).updateLastSeenTimestamp(anyString(), any());
                verify(repository, never()).createLastSeenTimestamp(anyString(), any());
        }
}
