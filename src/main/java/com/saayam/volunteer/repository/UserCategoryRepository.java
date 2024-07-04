package com.saayam.volunteer.repository;

import com.saayam.volunteer.model.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserCategoryRepository extends JpaRepository<UserCategory, Integer> {
}
