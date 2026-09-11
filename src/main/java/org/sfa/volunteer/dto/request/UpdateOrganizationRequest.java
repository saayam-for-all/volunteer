package org.sfa.volunteer.dto.request;

import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

@Builder
public record UpdateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 125, message = "Organization name must be 125 characters or fewer")
        String orgName,

        String orgType,
        String orgSize,
        String street,
        String cityName,
        String stateId,

        @NotBlank(message = "Zip code is required")
        @Size(max = 10, message = "Zip code must be 10 characters or fewer")
        String zipCode,

        @Pattern(regexp = "^[+]?[0-9\\s()-]{7,20}$", message = "Invalid phone number")
        String phone,

        @Email(message = "Invalid email format")
        String email,

        @URL(message = "Invalid URL")
        String webUrl,

        String mission,
        List<String> categoryIds
) {}