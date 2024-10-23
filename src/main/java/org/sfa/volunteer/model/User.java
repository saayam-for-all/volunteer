package org.sfa.volunteer.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private String id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @NotBlank(message = "Full name cannot be blank.")
    @Column(name = "full_name")
    private String fullName;

    @Email
//    @NotBlank(message = "Email address cannot be null.")
    @Column(name = "primary_email_address")
    private String primaryEmailAddress;

    //    @NotBlank(message = "Phone number cannot be blank")
    @Column(name = "primary_phone_number")
    private String primaryPhoneNumber;

    @NotBlank(message = "Time zone cannot be blank.")
    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "profile_picture_path")
    private String profilePicturePath;

    @Column(name = "addr_ln1")
    private String addressLine1;

    @Column(name = "addr_ln2")
    private String addressLine2;

    @Column(name = "addr_ln3")
    private String addressLine3;

    @Column(name = "city_name")
    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "gender")
    private String gender;

    @Column(name = "last_location")
    private String lastLocation;

    @Column(name = "language_1")
    private String language1;

    @Column(name = "language_2")
    private String language2;

    @Column(name = "language_3")
    private String language3;

    @Column(name = "promotion_wizard_stage")
    private Integer volunteerStage;;

    @Column(name = "promotion_wizard_last_update_date")
    private ZonedDateTime volunteerUpdateDate;

    @Column(name = "last_update_date")
    private ZonedDateTime lastUpdateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    @JsonBackReference
    private State state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    @JsonBackReference
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_status_id", nullable = false)
    @JsonBackReference
    private UserStatus userStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_category_id", nullable = false)
    @JsonBackReference
    private UserCategory userCategory;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserAdditionalDetail additionalDetail;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Volunteer volunteer;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", fullName='" + fullName + '\'' +
                ", emailAddress='" + primaryEmailAddress + '\'' +
                ", phoneNumber='" + primaryPhoneNumber + '\'' +
                ", timeZone='" + timeZone + '\'' +
                ", profilePicturePath='" + profilePicturePath + '\'' +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", addressLine3='" + addressLine3 + '\'' +
                ", city='" + city + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", lastUpdateDate=" + lastUpdateDate +
                ", state=" + state +
                ", country=" + country +
                ", userStatus=" + userStatus +
                ", userCategory=" + userCategory +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(firstName, user.firstName) &&
                Objects.equals(middleName, user.middleName) &&
                Objects.equals(lastName, user.lastName) &&
                Objects.equals(fullName, user.fullName) &&
                Objects.equals(primaryEmailAddress, user.primaryEmailAddress) &&
                Objects.equals(primaryPhoneNumber, user.primaryPhoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, middleName, lastName, fullName, primaryEmailAddress, primaryPhoneNumber);
    }
}