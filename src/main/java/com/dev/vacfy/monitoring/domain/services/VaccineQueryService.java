package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;

import java.util.List;

public interface VaccineQueryService {
    /** Catálogo de vacunas ordenado por nombre (sin el perfil genérico por defecto). */
    List<VaccineProfile> getVaccines();
}
