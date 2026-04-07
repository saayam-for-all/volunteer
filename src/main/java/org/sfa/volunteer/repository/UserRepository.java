package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.User;
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

    // Find users by firstName and lastName (case-insensitive) - used for doesUserExist API
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.country WHERE LOWER(u.firstName) = LOWER(:firstName) AND LOWER(u.lastName) = LOWER(:lastName)")
    List<User> findByFirstNameAndLastNameIgnoreCase(@Param("firstName") String firstName, @Param("lastName") String lastName);

    Optional<User> findFirstByPrimaryEmailAddressOrderByLastUpdateDateDesc(String email);
    // fallback if lastUpdateDate is null/old data
    Optional<User> findFirstByPrimaryEmailAddressOrderByIdDesc(String email);
}
