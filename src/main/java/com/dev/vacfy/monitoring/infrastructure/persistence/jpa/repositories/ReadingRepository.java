package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.monitoring.domain.model.aggregates.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, Long> {
    List<Reading> findTop1000ByContenedorAndReceivedAtBetweenOrderByReceivedAtDesc(String contenedor, Instant from, Instant to);

    Optional<Reading> findFirstByContenedorOrderByReceivedAtDesc(String contenedor);

    @Query("select distinct r.contenedor from Reading r where r.receivedAt >= :since")
    List<String> findContenedoresSince(@Param("since") Instant since);
}
