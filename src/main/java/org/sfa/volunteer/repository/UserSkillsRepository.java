package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.entity.UserSkills;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSkillsRepository extends JpaRepository<UserSkills, Long> {

    @Query("SELECT us FROM UserSkills us WHERE us.skillId = :skillId")
    List<UserSkills> findBySkillId(@Param("skillId") Integer skillId);
}
