package org.sfa.volunteer.model;


import lombok.Getter;

@Getter
public class VolunteerRequest {
    private int requestId;
    private String requestName;
    private AssistanceMode assistanceMode;
    private Category category;
    private String location;
    private String description;
}
