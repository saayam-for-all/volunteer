package org.sfa.volunteer.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "volunteer_details")
@Lazy
public class Volunteer {
    @Id
    @Column(name = "user_id")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "terms_and_conditions")
    private Boolean termsAndConditions;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "govt_id_path1")
    private String govtIdPath1;

    @Column(name = "govt_id_path2")
    private String govtIdPath2;

    @Column(name = "path1_updated_at")
    private LocalDateTime path1UpdatedAt;

    @Column(name = "path2_updated_at")
    private LocalDateTime path2UpdatedAt;

    // jsonb columns
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "availability_days")
    private Object availabilityDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "availability_times")
    private Object availabilityTimes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at", insertable = false, updatable = false)
    private LocalDateTime lastUpdatedAt;

    @Override
    public String toString() {
        return "Volunteer{" +
                "id='" + id + '\'' +
                ", user=" + user +
                ", termsAndConditions=" + termsAndConditions +
                ", termsAcceptedAt=" + termsAcceptedAt +
                ", govtIdPath1='" + govtIdPath1 + '\'' +
                ", govtIdPath2='" + govtIdPath2 + '\'' +
                ", path1UpdatedAt=" + path1UpdatedAt +
                ", path2UpdatedAt=" + path2UpdatedAt +
                ", availabilityDays=" + availabilityDays +
                ", availabilityTimes=" + availabilityTimes +
                ", createdAt=" + createdAt +
                ", lastUpdatedAt=" + lastUpdatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Volunteer volunteer = (Volunteer) o;
        return Objects.equals(id, volunteer.id) && Objects.equals(user, volunteer.user) && Objects.equals(termsAndConditions, volunteer.termsAndConditions) && Objects.equals(termsAcceptedAt, volunteer.termsAcceptedAt) && Objects.equals(govtIdPath1, volunteer.govtIdPath1) && Objects.equals(govtIdPath2, volunteer.govtIdPath2) && Objects.equals(path1UpdatedAt, volunteer.path1UpdatedAt) && Objects.equals(path2UpdatedAt, volunteer.path2UpdatedAt) && Objects.equals(availabilityDays, volunteer.availabilityDays) && Objects.equals(availabilityTimes, volunteer.availabilityTimes) && Objects.equals(createdAt, volunteer.createdAt) && Objects.equals(lastUpdatedAt, volunteer.lastUpdatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, termsAndConditions, termsAcceptedAt, govtIdPath1, govtIdPath2, path1UpdatedAt, path2UpdatedAt, availabilityDays, availabilityTimes, createdAt, lastUpdatedAt);
    }
}