package org.sfa.volunteer.repository;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolunteerUserAvailabilityRepository extends JpaRepository<VolunteerUserAvailability, Long> {

//    Volunteer findVolunteerByUserId(String userId);
    @Query("SELECT u FROM VolunteerUserAvailability u WHERE u.user > :userId")
    List<VolunteerUserAvailability> findUserAvailability(@Param("userId") String userId);
//    List<VolunteerUserAvailability> findUserAvailability(String userId);
}
