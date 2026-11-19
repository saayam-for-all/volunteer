package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolunteerUserAvailabilityRepository extends JpaRepository<VolunteerUserAvailability, Integer> {

    List<VolunteerUserAvailability> findByUser_IdOrderByIdAsc(String userId);

    @Modifying
    void deleteByUser_Id(String userId);
}
