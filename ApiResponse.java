package com.volunteer.microservice.VolunteerService.Payload;

import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class ApiResponse {
    private String Message;
    private boolean Result;
    private HttpStatus status;
}
