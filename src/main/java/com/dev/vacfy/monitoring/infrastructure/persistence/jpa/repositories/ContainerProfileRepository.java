package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerProfileRepository extends JpaRepository<ContainerProfile, String> { }
