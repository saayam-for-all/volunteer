package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.entity.UserAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAvailabilityRepository extends JpaRepository<UserAvailability, Integer> {

    @Query("SELECT ua FROM UserAvailability ua WHERE ua.userId = :userId")
    List<UserAvailability> findByUserId(@Param("userId") String userId);
}
