package com.volunteer.microservice.VolunteerService.repositories.services.impl.Volunteer_Service;

import com.volunteer.microservice.VolunteerService.Exceptions.ResourceNotFoundException;
import com.volunteer.microservice.VolunteerService.entities.Volunteer;
import com.volunteer.microservice.VolunteerService.repositories.Volunteer_Repository;
import com.volunteer.microservice.VolunteerService.repositories.services.Volunteer_Services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VolunteerServiceImple implements Volunteer_Services {
    @Autowired
    private Volunteer_Repository volunteerRepository;
    @Override
    public Volunteer saveVolunteer(Volunteer volunteer) {

        //generating unique volunteer id
        String random_volunteer_ID = UUID.randomUUID().toString();
        Volunteer.setVolunteerID(random_volunteer_ID);
        return volunteerRepository.save(volunteer);
    }

    @Override
    public List<Volunteer> getallVolunteer() {
        return volunteerRepository.findAll();
    }

    @Override
    public Volunteer getVolunteer(String Volunteer_ID) {
        return volunteerRepository.findById(Volunteer_ID).orElseThrow(() -> new ResourceNotFoundException("Volunteer with given ID cannot be found"));
    }
}
