package org.sfa.volunteer.controller;

import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.VolunteerRequest;
import org.sfa.volunteer.service.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/volunteer")
public class VolunteerController {

    private VolunteerService volunteerService;

    @Autowired
    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @PostMapping("/request")
    public boolean requestVolunteer(@RequestBody VolunteerRequest volunteerRequest) {
        return volunteerService.addVolunteerRequest(volunteerRequest);
    }

    @PostMapping
    public boolean addVolunteer(@RequestBody Volunteer volunteer) {
        return volunteerService.addVolunteer(volunteer);
    }

    @GetMapping("/matches")
    public List<Volunteer> getVolunteers(@RequestBody VolunteerRequest volunteerRequest) {
        return volunteerService.matchVolunteers(volunteerRequest);
    }


}
