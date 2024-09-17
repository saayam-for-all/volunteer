package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    @Query("SELECT u.language1 from User u where u.id = :requesterId")
    String getLanguagePreference1(String requesterId);

    @Query("SELECT u.language2 from User u where u.id = :requesterId")
    String getLanguagePreference2(String requesterId);

    @Query("SELECT u.language3 from User u where u.id = :requesterId")
    String getLanguagePreference3(String requesterId);
}
