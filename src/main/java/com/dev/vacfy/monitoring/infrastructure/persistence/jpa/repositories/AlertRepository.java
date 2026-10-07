package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findFirstByContenedorAndTypeAndStatusInOrderByStartedAtDesc(String contenedor, AlertType type, Collection<AlertStatus> statuses);

    List<Alert> findByContenedorAndStatusIn(String contenedor, Collection<AlertStatus> statuses);

    List<Alert> findByLotIdAndStatusIn(Long lotId, Collection<AlertStatus> statuses);

    List<Alert> findByStatusIn(Collection<AlertStatus> statuses);

    List<Alert> findTop200ByStatusInOrderByStartedAtDesc(Collection<AlertStatus> statuses);

    List<Alert> findTop200ByContenedorAndStatusInOrderByStartedAtDesc(String contenedor, Collection<AlertStatus> statuses);

    List<Alert> findTop200ByContenedorInAndStatusInOrderByStartedAtDesc(Collection<String> contenedores, Collection<AlertStatus> statuses);
}
