package org.sfa.volunteer.repository;

import java.sql.Timestamp;

import org.sfa.volunteer.entities.UserNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationStatusRepository
                extends JpaRepository<UserNotificationStatus, Long> {

        UserNotificationStatus findByUserId(String userId);

        @Query("SELECT u.watermarkTimestamp FROM UserNotificationStatus u WHERE u.userId = :userId")
        Timestamp getLastSeenTimestamp(String userId);

        @Query("""
                        SELECT COUNT(u)
                        FROM UserNotificationStatus u
                        WHERE u.userId = :userId
                        """)
        int existsByUserId(@Param("userId") String userId);

        @Modifying
        @Query("""
                        UPDATE UserNotificationStatus u
                        SET u.watermarkTimestamp = :watermarkTimestamp
                        WHERE u.userId = :userId
                        """)
        int updateLastSeenTimestamp(String userId, Timestamp watermarkTimestamp);

        @Modifying
        @Query("INSERT INTO UserNotificationStatus (userId, watermarkTimestamp) VALUES (:userId, :watermarkTimestamp)")
        int createLastSeenTimestamp(String userId, Timestamp watermarkTimestamp);
}