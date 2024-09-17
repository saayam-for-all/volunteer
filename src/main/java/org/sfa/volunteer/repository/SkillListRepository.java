package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.entity.SkillList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillListRepository extends JpaRepository<SkillList, Long> {

    @Query("SELECT s FROM SkillList s WHERE s.requestCategoryId = :requestCategoryId")
    List<SkillList> findByRequestCategoryId(@Param("requestCategoryId") Integer requestCategoryId);
}
