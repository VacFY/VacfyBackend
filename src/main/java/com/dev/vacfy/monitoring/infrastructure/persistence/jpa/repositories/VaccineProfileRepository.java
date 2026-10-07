package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VaccineProfileRepository extends JpaRepository<VaccineProfile, Long> {
    Optional<VaccineProfile> findByName(String name);

    boolean existsByName(String name);
}
