package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.Organization;
import org.sfa.volunteer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByUser(User user);

    Optional<Organization> findByUserId(String userId);

}
