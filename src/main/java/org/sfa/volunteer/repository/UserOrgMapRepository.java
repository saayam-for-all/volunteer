package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.UserOrgMap;
import org.sfa.volunteer.model.UserOrgMapId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOrgMapRepository extends JpaRepository<UserOrgMap, UserOrgMapId> {

    List<UserOrgMap> findByUserId(String userId);
}