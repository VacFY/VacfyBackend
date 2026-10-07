package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotCodeReading;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotView;

import java.util.List;
import java.util.Optional;

public interface LotQueryService {
    /** Lee un código escaneado o escrito y busca su vacuna. No guarda nada. */
    LotCodeReading read(String code);

    /** Lotes del termo ordenados por vencimiento: ACTIVE y, si se pide, también EXPIRED. */
    List<LotView> getContainerLots(String contenedor, boolean includeExpired);

    /** Lotes ACTIVE de todos los termos que vencen en N días o menos (ya vencidos incluidos). */
    List<LotView> getExpiringLots(int days);

    Optional<VaccineLot> getLot(Long lotId);
}
