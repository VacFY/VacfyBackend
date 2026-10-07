package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.monitoring.domain.model.aggregates.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, Long> {
    List<Reading> findTop1000ByContenedorAndReceivedAtBetweenOrderByReceivedAtDesc(String contenedor, Instant from, Instant to);
}
