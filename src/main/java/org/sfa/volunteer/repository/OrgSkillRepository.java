package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.OrgSkill;
import org.sfa.volunteer.model.OrgSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrgSkillRepository extends JpaRepository<OrgSkill, OrgSkillId> {
    List<OrgSkill> findByOrgId(String orgId);

    // Bulk delete that runs immediately, so re-adding an existing category in
    // the same transaction doesn't collide with an entity still marked "removed".
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from OrgSkill s where s.orgId = :orgId")
    void deleteByOrgId(@Param("orgId") String orgId);
}