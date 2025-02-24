package org.sfa.volunteer.repository;

import java.util.Optional;

import org.sfa.volunteer.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {
    
	Optional<Country> findByCountryName(String name);

}
