package org.sfa.volunteer.repository;

import java.util.List;

import org.sfa.volunteer.model.UserSkillId;
import org.sfa.volunteer.model.UserSkills;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkills, UserSkillId> {

    List<UserSkills> findByIdCatId(String skills);

    List<UserSkills> findByIdUserId(String userId);
}
