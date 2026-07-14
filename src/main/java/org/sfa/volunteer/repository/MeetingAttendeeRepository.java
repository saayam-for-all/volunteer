package org.sfa.volunteer.repository;

import org.sfa.volunteer.entities.MeetingAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, Long> {
    List<MeetingAttendee> findByUserId(String userId);
    List<MeetingAttendee> findByUserEmail(String userEmail);
}