package org.sfa.volunteer.model;

import org.hibernate.annotations.ColumnTransformer;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    @Column(name = "org_id", insertable = false, updatable = false)
    private String orgId;
    
    @Column(name="org_name")
    private String orgName;

    @Column(name="street")
    private String street;

    @Column(name="city_name")
    private String cityName;

    @Column(name="zip_code")
    private String zipCode;	

    @Column(name="mission")
    private String mission;	

    @Column(name="web_url")
    private String webUrl;

    @Column(name="phone")
    private String phone;

    @Column(name="email")
    private String email;
    
    @Column(name="org_rating")
    private Integer orgRating;

    @Column(name="is_collaborator")
    private Boolean isCollaborator;

    @Column(name="state_id")
    private String stateId;

    @Column(name = "org_type")
    @ColumnTransformer(write = "?::org_type_enum")
    private String orgType;

    @Column(name = "org_size")
    @ColumnTransformer(write = "?::org_size_enum")
    private String orgSize;
}
