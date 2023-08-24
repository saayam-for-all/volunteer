package com.volunteer.microservice.VolunteerService.repositories.services;

import com.volunteer.microservice.VolunteerService.entities.Volunteer;

import java.util.List;

public interface Volunteer_Services {
    //operations

    static Volunteer saveVolunteer(Volunteer volunteer) {
        return null;
    }

    //Get all
    List<Volunteer> getallVolunteer();

    //get one
    static Volunteer getVolunteer(String Volunteer_ID) {
        return null;
    }
}
