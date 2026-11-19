package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    List<Organization> findByOrgNameContainingIgnoreCase(String name);
}