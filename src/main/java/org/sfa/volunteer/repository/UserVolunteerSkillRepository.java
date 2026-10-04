package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.UserVolunteerSkill;
import org.sfa.volunteer.model.UserVolunteerSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserVolunteerSkillRepository extends JpaRepository<UserVolunteerSkill, UserVolunteerSkillId> {
    List<UserVolunteerSkill> findByUser_Id(String userId);
}
