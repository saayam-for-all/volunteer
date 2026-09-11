package org.sfa.volunteer.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sfa.volunteer.dto.response.NotificationResponse;
import org.sfa.volunteer.entities.Notifications;

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

    @Query(value = """
                SELECT n.notification_id,
                n.status,
                nt.type_name,
                n.message,
                n.created_at
                FROM notifications n
                JOIN notification_types nt ON n.type_id = nt.type_id
                WHERE n.user_id = :userId
                ORDER BY n.created_at DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM notifications n
            WHERE n.user_id = :userId
            """, nativeQuery = true)
    Page<Object[]> findNotifications(String userId, Pageable pageable);

}