package org.sfa.volunteer.service;

import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.VolunteerRequest;

import java.util.List;

public interface VolunteerService {
    boolean addVolunteerRequest(VolunteerRequest volunteerRequest);
    List<Volunteer> matchVolunteers(VolunteerRequest volunteerRequest);
    boolean addVolunteer(Volunteer volunteer);
}
