package com.saayam.volunteer.model;

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

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "country")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "country_id", updatable = false, nullable = false)
    private Integer countryId;

    @Column(name = "country_name")
    private String countryName;

    @Column(name = "phone_country_code")
    private String phoneCountryCode;

    @Column(name = "last_update_date")
    private ZonedDateTime lastUpdateDate;
}
