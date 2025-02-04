package org.sfa.volunteer.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import java.time.ZonedDateTime;

@Builder
public record VolunteerRequest(
        @NotBlank Integer step,
        @NotBlank String userId,
        Boolean termsAndConditions,
        ZonedDateTime tcUpdateDate,
        String govtIdFilename,
        ZonedDateTime govtUpdateDate,
        String skills,
        Boolean notification,
        Boolean isCompleted,
        ZonedDateTime completedDate) {
}
