package com.volunteer.microservice.VolunteerService.repositories;

import com.volunteer.microservice.VolunteerService.entities.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Volunteer_Repository extends JpaRepository<Volunteer, String> {
    // any custom implmentations or any custom query is done here
}
