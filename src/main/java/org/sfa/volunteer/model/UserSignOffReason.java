package org.sfa.volunteer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_signoff")
public class UserSignOffReason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signoff_id", nullable = false)
    private Integer id;

    @Column(name = "reason", length = 250)
    private String reason;

    public UserSignOffReason(String reason) {
        this.reason = reason;
    }
}
