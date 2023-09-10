package com.saayam.volunteer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @Column(name = "user_id")
    private BigInteger id;

    @Column(name = "idnty_typ_id")
    private BigInteger idnty_typ_id;

    @Column(name = "frst_Nm")
    private String firstName;

    @Column(name = "mdl_nm")
    private String middleName;

    @Column(name = "lst_Nm")
    private String lastName;

    @Column(name = "Eml_id")
    private String emailId;

    @Column(name = "phn_nbr")
    private BigInteger phoneNumber;

    @Column(name = "Addr_ln1")
    private String addressLine1;

    @Column(name = "Addr_ln2")
    private String addressLine2;

    @Column(name = "addr_ln3")
    private String addressLine3;

    @Column(name = "cty")
    private String city;

    @Column(name = "cntry_id")
    private String countryId;

    @Column(name = "state_id")
    private BigInteger stateId;

    @Column(name = "zip_cd")
    private BigInteger zipCode;

    @Column(name = "geo_code")
    private String geoCode;

    @Column(name = "Regd_dt")
    private Date registeredDate;

    @Column(name = "usr_sts_id")
    private BigInteger userSTSId;

    @Column(name = "usr_sts_dt")
    private Date userSTSDate;

    @Column(name = "emergency_avbity_ind")
    private String emergencyInd;

    @Column(name = "cntc_mbr")
    private String cntcMbr;

}
