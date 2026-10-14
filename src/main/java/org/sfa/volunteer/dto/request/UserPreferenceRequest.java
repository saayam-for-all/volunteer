package org.sfa.volunteer.dto.request;
import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record UserPreferenceRequest(
        String language1,
        String language2,
        String language3,
        @Email String secondaryEmail1,
        @Email String secondaryEmail2,
        String secondaryPhone1,
        String secondaryPhone2) {
}