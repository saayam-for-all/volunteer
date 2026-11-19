package org.sfa.volunteer.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.sfa.volunteer.enums.ActionStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sfa.volunteer.entities.Notifications;

@Repository
public interface NotificationsRepository
        extends JpaRepository<Notifications, Long> {

    // Keep list, pagination totals, and bell counts consistent. Retain workflow
    // records for history, but hide requests claimed by another volunteer.
    String VISIBLE_TO_USER = """
            n.userId = :userId
            AND (n.actionStatus IS NULL OR (
                n.actionStatus <> org.sfa.volunteer.enums.ActionStatus.REQUEST_TAKEN
                AND NOT EXISTS (
                    SELECT winner.notificationId FROM Notifications winner
                    WHERE winner.requestId = n.requestId
                    AND winner.actionStatus = org.sfa.volunteer.enums.ActionStatus.ACCEPTED
                    AND winner.userId <> n.userId
                )
            ))
            """;

    @Query("SELECT COUNT(n) FROM Notifications n WHERE " + VISIBLE_TO_USER)
    int countAllNotifications(String userId);

    @Query("SELECT COUNT(n) FROM Notifications n WHERE " + VISIBLE_TO_USER
            + " AND n.createDttm > :watermarkTimestamp")
    int countNewNotifications(String userId, Timestamp watermarkTimestamp);

    @Query(
           value = """
           SELECT n.notificationId,
           n.status,
           nt.typeName,
           n.message,
           n.createDttm,
           n.requestId,
           n.actionStatus
           FROM Notifications n
           JOIN NotificationTypes nt ON n.typeId = nt.typeId
           WHERE
        """ + VISIBLE_TO_USER + " ORDER BY n.createDttm DESC",
           countQuery = "SELECT COUNT(n) FROM Notifications n WHERE " + VISIBLE_TO_USER
        )
   Page<Object[]> findNotifications(@Param("userId") String userId,Pageable pageable);

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
