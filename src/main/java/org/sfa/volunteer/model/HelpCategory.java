package org.sfa.volunteer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "help_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpCategory {

    @Id
    @Column(name = "cat_id")
    private String catId;

    @Column(name = "cat_name", nullable = false)
    private String catName;

    @Column(name = "cat_desc", nullable = false)
    private String catDesc;
}
