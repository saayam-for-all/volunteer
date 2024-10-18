package org.sfa.volunteer.repository;
import org.sfa.volunteer.model.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VolunteerUserAvailabilityRepository extends JpaRepository<Volunteer, String> {

    Volunteer findVolunteerByUserId(String userId);
}
