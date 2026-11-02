package org.sfa.volunteer.repository;

import java.sql.Timestamp;
import java.util.List;

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

    @Query("SELECT COUNT(n) FROM Notifications n WHERE n.userId = :userId")
    int countAllNotifications(String userId);

    @Query("""
            SELECT COUNT(n)
            FROM Notifications n
            WHERE n.userId = :userId
            AND n.createDttm > :watermarkTimestamp
            """)
    int countNewNotifications(String userId, Timestamp watermarkTimestamp);

    /*@Query(value = """
                SELECT n.notificationId,
                n.status,
                nt.typeName,
                n.message,
                n.createDttm
                FROM Notifications n
                JOIN NotificationTypes nt ON n.typeId = nt.typeId
                WHERE n.userId = :userId
                ORDER BY n.createDttm DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM Notifications n
            WHERE n.userId = :userId
            """, nativeQuery = true)
    Page<Object[]> findNotifications(String userId, Pageable pageable);
   */
    @Query(
           value = """
           SELECT n.notificationId,
           n.status,
           nt.typeName,
           n.message,
           n.createDttm
           FROM Notifications n
           JOIN NotificationTypes nt ON n.typeId = nt.typeId
           WHERE n.userId = :userId
           ORDER BY n.createDttm DESC
        """, countQuery = """
        SELECT COUNT(n.notificationId)
        FROM Notifications n
        WHERE n.userId = :userId
        """
        )
   Page<Object[]> findNotifications(@Param("userId") String userId,Pageable pageable);
}