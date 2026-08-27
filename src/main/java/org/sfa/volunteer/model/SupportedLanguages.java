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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "supporting_languages")
public class SupportedLanguages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "language_id")
    private Long languageId; // Maps to bigint

    @Column(name = "language_name", length = 64)
    private String languageName; // Maps to char varying 64

    @Column(name = "iso_code", columnDefinition = "char(2)")
    private String isoCode; // Maps to char 2

    @Column(name = "local_code", length = 10)
    private String localCode; // Maps to char varying 10

    @Column(name = "writing_direction", length = 3)
    private String writingDirection; // Maps to char varying 3

    @Column(name = "total_speakers_m", precision = 10, scale = 1)
    private BigDecimal totalSpeakers; // Maps to num 10

    @Column(name = "is_active")
    private Boolean isActive; // Maps to boolean

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime createdAt; 

    @Column(name = "last_updated_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime lastUpdatedAt;

}