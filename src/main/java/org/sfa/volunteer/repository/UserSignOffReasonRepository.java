package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.UserSignOffReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSignOffReasonRepository extends JpaRepository<UserSignOffReason,Integer> {
}
