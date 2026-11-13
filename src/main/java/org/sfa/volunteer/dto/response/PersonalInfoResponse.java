package org.sfa.volunteer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonalInfoResponse {
    private String dateOfBirth;
    private String gender;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String zipCode;
    private String secondaryEmail;
    private String secondaryPhone;
}
