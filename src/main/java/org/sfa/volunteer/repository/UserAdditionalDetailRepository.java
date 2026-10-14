package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.UserAdditionalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAdditionalDetailRepository extends JpaRepository<UserAdditionalDetail, Long> {
    @Query("SELECT uad FROM UserAdditionalDetail uad WHERE uad.user.id = :userId")
    UserAdditionalDetail findByUserId(@Param("userId") String userId);
}
