package org.sfa.volunteer.entities;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_notification_status")
public class UserNotificationStatus {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "last_accessed_at")
    private Timestamp watermarkTimestamp;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getWatermarkTimestamp() {
        return watermarkTimestamp;
    }

    public void setWatermarkTimestamp(Timestamp watermarkTimestamp) {
        this.watermarkTimestamp = watermarkTimestamp;
    }
}