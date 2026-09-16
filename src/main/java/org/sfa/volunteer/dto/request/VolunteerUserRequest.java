package org.sfa.volunteer.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record VolunteerUserRequest (
    @NotBlank(message = "User ID cannot be blank") 
    String userId){

}