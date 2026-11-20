package org.sfa.volunteer.repository;

import java.util.Collection;
import java.util.List;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface VolunteerUserAvailabilityRepository extends JpaRepository<VolunteerUserAvailability, Integer> {

    List<VolunteerUserAvailability> findByUser_IdOrderByIdAsc(String userId);

    List<VolunteerUserAvailability> findByUser_IdInOrderByIdAsc(Collection<String> userIds);

    @Modifying
    void deleteByUser_Id(String userId);
}
