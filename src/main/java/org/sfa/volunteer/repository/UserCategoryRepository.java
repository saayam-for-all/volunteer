package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.entity.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCategoryRepository extends JpaRepository<UserCategory, Integer> {
}
