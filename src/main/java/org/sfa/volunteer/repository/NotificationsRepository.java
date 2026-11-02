package org.sfa.volunteer.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sfa.volunteer.dto.response.NotificationResponse;
import org.sfa.volunteer.entities.Notifications;
import org.sfa.volunteer.enums.ActionStatus;

@Repository
public interface NotificationsRepository
                extends JpaRepository<Notifications, Long> {

        @Query("SELECT COUNT(n) FROM Notifications n WHERE n.userId = :userId")
        int countAllNotifications(String userId);

        @Query("""
                        SELECT COUNT(n)
                        FROM Notifications n
                        WHERE n.userId = :userId
                        AND n.createDttm > :watermarkTimestamp
                        """)
        int countNewNotifications(String userId, Timestamp watermarkTimestamp);

        @Query("""
                        SELECT new org.sfa.volunteer.dto.response.NotificationResponse(
                            n.notificationId,
                            n.status,
                            nt.typeName,
                            n.message,
                            n.createDttm,
                            n.requestId,
                            n.actionStatus
                        )
                        FROM Notifications n
                        JOIN NotificationTypes nt on n.typeId = nt.typeId
                        WHERE n.userId = :userId
                        ORDER BY n.createDttm DESC
                        """)
        List<NotificationResponse> findNotifications(String userId, Pageable pageable);

        Optional<Notifications> findByNotificationIdAndUserId(Integer notificationId, String userId);

        // Scalar projections read the locked database state rather than an entity
        // already cached by the ownership lookup in the same transaction.
        @Query(value = """
                        SELECT notification_id AS notificationId, user_id AS userId,
                               action_status AS actionStatus
                        FROM notifications
                        WHERE req_id = :requestId
                        ORDER BY notification_id
                        FOR UPDATE
                        """, nativeQuery = true)
        List<RequestNotificationState> lockByRequestId(@Param("requestId") String requestId);

        interface RequestNotificationState {
                Integer getNotificationId();
                String getUserId();
                String getActionStatus();
        }

        @Modifying
        @Query("""
                        UPDATE Notifications n SET n.actionStatus = :takenStatus
                        WHERE n.requestId = :requestId
                        AND n.notificationId <> :acceptedNotificationId
                        AND n.actionStatus = :pendingStatus
                        """)
        int markOtherPendingNotificationsTaken(
                        @Param("requestId") String requestId,
                        @Param("acceptedNotificationId") Integer acceptedNotificationId,
                        @Param("pendingStatus") ActionStatus pendingStatus,
                        @Param("takenStatus") ActionStatus takenStatus);

        @Modifying
        @Query("""
                        UPDATE Notifications n
                        SET n.actionStatus = :newStatus
                        WHERE n.notificationId = :notificationId
                        AND n.userId = :userId
                        AND n.actionStatus = :expectedStatus
                        """)
        int updateActionStatusConditionally(
                        @Param("notificationId") Integer notificationId,
                        @Param("userId") String userId,
                        @Param("expectedStatus") ActionStatus expectedStatus,
                        @Param("newStatus") ActionStatus newStatus);

}
