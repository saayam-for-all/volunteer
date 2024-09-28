package org.sfa.volunteer.dto.response;

import lombok.Builder;

@Builder
public record VolunteerProfileResponse(
    int priorityNum,
    String id,
    String fullName,
    String emailAddress,
    String phoneNumber){
}
