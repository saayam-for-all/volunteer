package org.sfa.volunteer.service.impl;

import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VolunteerService {

    @Autowired
    VolunteerRepository volunteerRepository;
    public List<Volunteer> getAllVolunteers() {

    }
}
