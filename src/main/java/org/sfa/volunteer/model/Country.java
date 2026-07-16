package org.sfa.volunteer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "country")
public class Country {


    @Id
    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "country_name", length = 100)
    private String countryName;

    @Column(name = "phone_code", length = 5)
    private String phoneCode;

    @Column(name = "country_code", length = 6)
    private String countryCode;

    @Column(name = "is_eu_member")
    private Boolean isEuMember;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;


}
