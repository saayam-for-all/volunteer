package com.volunteer.microservice.VolunteerService.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "VolunteerMicroservices")
public class Volunteer {

    @Id
    @Column(name = "ID")
    private String Volunteer_ID;
    @Column(name = "First_Name")
    private String First_Name;
    @Column(name = "Last_Name")
    private String Last_Name;
    @Column(name = "Address")
    private String Address;
    @Column(name = "State")
    private String State;
    @Column(name = "City")
    private String City;
    @Column(name = "Postal_Code")
    private Long Postal_Code;
    @Column(name = "Country")
    private String Country;
    @Column(name = "Email")
    private String Email;
    @Column(name = "Contact")
    private Long Phone_Number;

}
