package com.saayam.volunteer.repository;

import com.saayam.volunteer.model.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StateRepository extends JpaRepository<State, Integer> {
}
