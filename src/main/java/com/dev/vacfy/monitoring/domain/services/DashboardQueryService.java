package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.ContainerSummary;

import java.util.List;

public interface DashboardQueryService {
    /** Un resumen por termo conocido (con telemetría, perfil, lotes o alertas), ordenado por código. */
    List<ContainerSummary> getSummary();
}
