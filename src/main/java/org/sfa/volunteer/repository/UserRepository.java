package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select u from User u
            where
              lower(coalesce(u.fullName, '')) like :q
              or lower(coalesce(u.primaryEmailAddress, '')) like :q
              or coalesce(u.primaryPhoneNumber, '') like :q
              or lower(concat(coalesce(u.firstName, ''), ' ', coalesce(u.lastName, ''))) like :q
              or lower(concat(coalesce(u.lastName, ''), ' ', coalesce(u.firstName, ''))) like :q
            """)
    Page<User> searchUsers(@Param("q") String q, Pageable pageable);
}
