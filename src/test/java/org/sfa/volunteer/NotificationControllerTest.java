
package org.sfa.volunteer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sfa.volunteer.controller.NotificationController;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.util.ResponseBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationControllerTest {

        @Mock
        private NotificationService notificationService;

        @Mock
        private ResponseBuilder responseBuilder;

        @Mock
        private UserNotificationStatusRepository repository;

        @InjectMocks
        private NotificationController notificationController;

        @BeforeEach
        void setup() {
                MockitoAnnotations.openMocks(this);
        }

        // ---------------------------------------------------------
        // TEST: GET NOTIFICATION COUNTS
        // ---------------------------------------------------------
        void testGetNotificationCounts() throws Exception {
                String userId = "U123";

                GetNotificationsResponse mockResponse = new GetNotificationsResponse(null,
                                null, null);

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

                SaayamResponse<GetNotificationsResponse> result = notificationController.getNotificationCounts(
                                GetNotificationsRequest.builder().userId(userId).build());

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals(200, result.statusCode());
                assertEquals(SaayamStatusCode.SUCCESS.getCode(), result.saayamCode());
                verify(notificationService,
                                times(1)).getNotificationCounts(any(GetNotificationsRequest.class));
        }

        @Test
        void testGetNotifications() throws Exception {
                String userId = "U123";
                int rowStart = 0;
                int rowEnd = 10;

                GetNotificationsResponse mockResponse = new GetNotificationsResponse(rowEnd,
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

                SaayamResponse<GetNotificationsResponse> result = notificationController.getNotifications(
                                GetNotificationsRequest.builder().userId(userId)
                                                .rowStart(rowStart).rowEnd(rowEnd).build());

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals(200, result.statusCode());
                verify(notificationService,
                                times(1)).getNotifications(any(GetNotificationsRequest.class));
        }

        @Test
        void testUpdateLastSeen() throws Exception {
                UpsertLastSeenRequest request = new UpsertLastSeenRequest(null);
                UpsertLastSeenResponse mockResponse = new UpsertLastSeenResponse(null, null, null);

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
        }

        // ---------------------------------------------------------
        // TEST 1: Successful update (rowsUpdated > 0)
        // ---------------------------------------------------------

        @Test
        void testUpdateLastSeen_Success() {

                // UpdateLastSeenRequest req = UpdateLastSeenRequest.builder()
                // .userId("U1")
                // .build();

                // // Mock repository update
                // when(repository.updateLastSeenTimestamp(eq("U1"), any(Timestamp.class)))
                // .thenReturn(1); // means row updated

                // // UpdateLastSeenResponse res = service.updateLastSeen(req);

                // // assertTrue(res.updated());
                // // assertNull(res.message());
        }

}
