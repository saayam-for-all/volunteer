package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    List<User> findByPrimaryEmailAddress(String email);

    List<User> findFirstByPrimaryEmailAddressIgnoreCase(String email);

    Optional<User> findFirstByPrimaryEmailAddressOrderByLastUpdateDateDesc(String email);
    // fallback if lastUpdateDate is null/old data
    Optional<User> findFirstByPrimaryEmailAddressOrderByIdDesc(String email);
}
