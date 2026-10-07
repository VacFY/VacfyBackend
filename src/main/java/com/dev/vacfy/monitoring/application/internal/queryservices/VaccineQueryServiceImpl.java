package com.dev.vacfy.monitoring.application.internal.queryservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.services.VaccineQueryService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VaccineQueryServiceImpl implements VaccineQueryService {
    private final VaccineProfileRepository vaccineProfileRepository;
    private final String defaultProfileName;

    public VaccineQueryServiceImpl(VaccineProfileRepository vaccineProfileRepository,
                                   @Value("${vacty.alerts.default-profile-name}") String defaultProfileName) {
        this.vaccineProfileRepository = vaccineProfileRepository;
        this.defaultProfileName = defaultProfileName;
    }

    @Override
    public List<VaccineProfile> getVaccines() {
        // El perfil genérico 2–8 °C no es una vacuna: sigue disponible en /vaccine-profiles
        return vaccineProfileRepository.findAll(Sort.by("name")).stream()
                .filter(vaccine -> !vaccine.getName().equals(defaultProfileName))
                .toList();
    }
}
