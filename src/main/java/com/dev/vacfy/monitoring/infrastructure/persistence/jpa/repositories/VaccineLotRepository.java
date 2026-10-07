package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface VaccineLotRepository extends JpaRepository<VaccineLot, Long> {
    List<VaccineLot> findByContenedorAndStatusInOrderByExpiryDateAsc(String contenedor, Collection<LotStatus> statuses);

    List<VaccineLot> findByStatusInOrderByExpiryDateAsc(Collection<LotStatus> statuses);

    List<VaccineLot> findByStatusAndExpiryDateLessThanEqualOrderByExpiryDateAsc(LotStatus status, LocalDate date);

    List<VaccineLot> findByVaccineIdAndStatus(Long vaccineId, LotStatus status);

    boolean existsByVaccineIdAndLotNumberAndContenedorAndStatus(Long vaccineId, String lotNumber, String contenedor, LotStatus status);
}
