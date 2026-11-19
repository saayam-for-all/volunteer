package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.StewardVolunteerResponse;

public interface StewardService {

    PaginationResponse<StewardVolunteerResponse> getVolunteers(
            Integer page,
            Integer size
    );
}