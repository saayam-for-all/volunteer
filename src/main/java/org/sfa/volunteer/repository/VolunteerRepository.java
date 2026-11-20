package org.sfa.volunteer.repository;

import java.util.Optional;
import org.sfa.volunteer.model.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, String> {
    interface LegacyAvailability {
        String getDays();
        String getTimes();
    }

    @Query(value = """
            SELECT CAST(availability_days AS text) AS "days", CAST(availability_times AS text) AS "times"
            FROM volunteer_details WHERE user_id = :userId
            """, nativeQuery = true)
    Optional<LegacyAvailability> findLegacyAvailabilityById(@Param("userId") String userId);

    @Modifying
    @Query(value = """
            UPDATE volunteer_details SET availability_days = NULL, availability_times = NULL
            WHERE user_id = :userId AND (availability_days IS NOT NULL OR availability_times IS NOT NULL)
            """, nativeQuery = true)
    void clearLegacyAvailability(@Param("userId") String userId);


    Volunteer findVolunteerByUserId(String userId);

    Optional<Volunteer> findById(String userId);

}
