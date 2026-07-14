package org.sfa.volunteer.repository;

import org.sfa.volunteer.entities.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByZoomMeetingId(String zoomMeetingId);
    List<Meeting> findByHostUserId(String hostUserId);
}