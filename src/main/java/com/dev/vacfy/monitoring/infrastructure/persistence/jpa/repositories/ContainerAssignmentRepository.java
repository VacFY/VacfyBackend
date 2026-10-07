package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerAssignmentRepository extends JpaRepository<ContainerAssignment, Long> {
    Optional<ContainerAssignment> findByContenedorAndHastaIsNull(String contenedor);

    List<ContainerAssignment> findByUserIdAndHastaIsNull(String userId);

    List<ContainerAssignment> findByHastaIsNull();

    List<ContainerAssignment> findByContenedorOrderByDesdeDesc(String contenedor);
}
