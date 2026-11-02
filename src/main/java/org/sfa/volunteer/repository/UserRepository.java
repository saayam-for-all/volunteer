package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    List<User> findByPrimaryEmailAddress(String email);

    List<User> findFirstByPrimaryEmailAddressIgnoreCase(String email);

    Optional<User> findFirstByPrimaryEmailAddressOrderByLastUpdateDateDesc(String email);
    // fallback if lastUpdateDate is null/old data
    Optional<User> findFirstByPrimaryEmailAddressOrderByIdDesc(String email);

    // Auth check for the profile-image path: confirms a *specific* user id
    // owns a given email, rather than resolving "the" user for an email (which
    // is ambiguous when two rows share an email -- see issue #165 follow-up on
    // duplicate-email rows). The caller already supplies the user id it
    // believes is correct; this just verifies that claim against this one row.
    boolean existsByIdAndPrimaryEmailAddressIgnoreCase(String id, String primaryEmailAddress);

    // Narrow read/write for the profile-image path: avoid hydrating or
    // saving the full User entity, which currently includes columns
    // (language_1/2/3, user_category_id) that no longer match the real
    // schema. See issue #165 follow-up on the broader schema drift.
    @Query("select u.profilePicturePath from User u where u.id = :userId")
    Optional<String> findProfilePicturePathById(@Param("userId") String userId);

    @Modifying
    @Query("update User u set u.profilePicturePath = :s3Uri, u.lastUpdateDate = :now where u.id = :userId")
    int updateProfilePicturePath(@Param("userId") String userId, @Param("s3Uri") String s3Uri,
            @Param("now") ZonedDateTime now);

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
