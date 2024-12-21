package org.sfa.volunteer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_additional_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdditionalDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "additional_detail_id")
    private Long additionalDetailId;

    @OneToOne
    @JoinColumn(name = "user_id",
                referencedColumnName = "user_id",
                nullable = false)
    private User user;

    @Column(name = "secondary_email_1")
    private String secondaryEmail1;

    @Column(name = "secondary_email_2")
    private String secondaryEmail2;

    @Column(name = "secondary_phone_1")
    private String secondaryPhone1;

    @Column(name = "secondary_phone_2")
    private String secondaryPhone2;
}
