package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.VolunteerDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerDetailsRepository extends JpaRepository<VolunteerDetails, Long> {
}
