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
                            n.createDttm
                        )
                        FROM Notifications n
                        JOIN NotificationTypes nt on n.typeId = nt.typeId
                        WHERE n.userId = :userId
                        ORDER BY n.createDttm DESC
                        """)
        List<NotificationResponse> findNotifications(String userId, Pageable pageable);

        Optional<Notifications> findByNotificationIdAndUserId(Integer notificationId, String userId);

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