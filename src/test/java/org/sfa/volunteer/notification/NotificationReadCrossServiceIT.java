package org.sfa.volunteer.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.NotificationResponse;
import org.sfa.volunteer.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads, through the Volunteer service's real notification APIs, the rows written by
 * the Request service's notification consumer in the same demo database.
 *
 * <p>Prerequisite: run the request service's {@code demo/run-demo.sh} first so the
 * notifications exist. Both services point at the same Postgres container.
 */
@SpringBootTest
@ActiveProfiles("demo")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Volunteer service reads notifications written by the request service")
class NotificationReadCrossServiceIT {

    private static final String VOL_ALEX = "SID-00-000-000-002";
    private static final String VOL_CASEY = "SID-00-000-000-004";

    @Autowired private NotificationService notificationService;

    @Test
    @Order(1)
    @DisplayName("Badge count shows the request-service notification as new")
    void badgeCountIsNew() {
        GetNotificationsResponse counts = notificationService.getNotificationCounts(
                GetNotificationsRequest.builder().userId(VOL_ALEX).build());

        assertThat(counts.totalCount()).isEqualTo(1);
        assertThat(counts.newNotificationsCount())
                .as("no watermark yet, so the notification is new")
                .isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("An unmatched volunteer sees nothing")
    void unmatchedVolunteerSeesNothing() {
        GetNotificationsResponse counts = notificationService.getNotificationCounts(
                GetNotificationsRequest.builder().userId(VOL_CASEY).build());

        assertThat(counts.totalCount()).isZero();
        assertThat(counts.newNotificationsCount()).isZero();
    }

    @Test
    @Order(3)
    @DisplayName("Listing returns the notification, tagged new for a first-time viewer")
    void listReturnsNotificationTaggedNew() {
        GetNotificationsResponse page = notificationService.getNotifications(
                GetNotificationsRequest.builder().userId(VOL_ALEX).rowStart(0).rowEnd(9).build());

        List<NotificationResponse> items = page.notifications();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).typeName()).isEqualTo("REQUEST_CREATED");
        assertThat(items.get(0).message()).contains("FOOD_ASSISTANCE");
        assertThat(items.get(0).status())
                .as("a first-time viewer must see 'new' here, matching the badge count")
                .isEqualTo("new");
    }

    @Test
    @Order(4)
    @DisplayName("After marking last-seen, the badge clears and the item reads as old")
    void watermarkClearsBadge() {
        notificationService.upsertLastSeen(
                UpsertLastSeenRequest.builder().userId(VOL_ALEX).build());

        GetNotificationsResponse counts = notificationService.getNotificationCounts(
                GetNotificationsRequest.builder().userId(VOL_ALEX).build());
        assertThat(counts.newNotificationsCount()).isZero();
        assertThat(counts.totalCount()).isEqualTo(1);

        GetNotificationsResponse page = notificationService.getNotifications(
                GetNotificationsRequest.builder().userId(VOL_ALEX).rowStart(0).rowEnd(9).build());
        assertThat(page.notifications().get(0).status()).isEqualTo("old");
    }
}
