package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.OrgSkill;
import org.sfa.volunteer.model.OrgSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrgSkillRepository extends JpaRepository<OrgSkill, OrgSkillId> {
    List<OrgSkill> findByOrgId(String orgId);
    void deleteByOrgId(String orgId);
}