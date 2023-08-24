package com.volunteer.microservice.VolunteerService.controllers;

import com.volunteer.microservice.VolunteerService.VolunteerServiceApplication;
import com.volunteer.microservice.VolunteerService.entities.Volunteer;
import com.volunteer.microservice.VolunteerService.repositories.services.Volunteer_Services;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/volunteer")
public class VolunteerController {

    //use any methods or create any methods

    public ResponseEntity<Volunteer> createVolunteer(@RequestBody Volunteer volunteer){
        Volunteer volunteer1 = Volunteer_Services.saveVolunteer(volunteer);
        return ResponseEntity.status(HttpStatus.CREATED).body(volunteer1);
    }
    @GetMapping("/{VolunteerID")
    public ResponseEntity<Volunteer> getSingleVolunteer(@PathVariable Long VolunteerID){
        Volunteer volunteer = Volunteer_Services.getVolunteer(String.valueOf(VolunteerID));
        return ResponseEntity.ok(volunteer);
    }
}
