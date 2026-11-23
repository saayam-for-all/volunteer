package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.UserSkill;
import org.sfa.volunteer.model.UserSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, UserSkillId> {
    void deleteByUserId(String userId);
}
