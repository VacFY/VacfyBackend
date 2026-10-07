package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.ContainerSummary;

import java.util.Collection;
import java.util.List;

public interface DashboardQueryService {
    /** Un resumen por termo conocido (registrado, o con telemetría, perfil, lotes o alertas), ordenado por código. */
    List<ContainerSummary> getSummary();

    /** Resumen de exactamente estos termos (aunque no tengan datos todavía), ordenado por código. */
    List<ContainerSummary> getSummary(Collection<String> containers);
}
