package org.sfa.volunteer.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
public record UserPreferenceRequest(
        @NotEmpty(message = "At least one preferred language ID must be provided")
        @Size(max = 3, message = "You can select a maximum of 3 preferred languages")
        List<Long> preferredLanguageIds,

        @Email String secondaryEmail1,
        @Email String secondaryEmail2,
        String secondaryPhone1,
        String secondaryPhone2) {
}