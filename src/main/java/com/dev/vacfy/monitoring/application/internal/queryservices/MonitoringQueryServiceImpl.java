package com.dev.vacfy.monitoring.application.internal.queryservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.aggregates.Reading;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.queries.GetAlertsQuery;
import com.dev.vacfy.monitoring.domain.model.queries.GetReadingsQuery;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertStatus;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ReadingRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProfileRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MonitoringQueryServiceImpl implements MonitoringQueryService {
    private final AlertRepository alertRepository;
    private final ReadingRepository readingRepository;
    private final VaccineProfileRepository vaccineProfileRepository;

    public MonitoringQueryServiceImpl(AlertRepository alertRepository, ReadingRepository readingRepository,
                                      VaccineProfileRepository vaccineProfileRepository) {
        this.alertRepository = alertRepository;
        this.readingRepository = readingRepository;
        this.vaccineProfileRepository = vaccineProfileRepository;
    }

    @Override
    public List<Alert> handle(GetAlertsQuery query) {
        Set<AlertStatus> statuses = parseStatuses(query.status());
        String contenedor = query.contenedor();
        if (contenedor == null || contenedor.isBlank()) {
            return alertRepository.findTop200ByStatusInOrderByStartedAtDesc(statuses);
        }
        return alertRepository.findTop200ByContenedorAndStatusInOrderByStartedAtDesc(contenedor.trim(), statuses);
    }

    @Override
    public List<Reading> handle(GetReadingsQuery query) {
        Instant to = query.to() != null ? query.to() : Instant.now();
        Instant from = query.from() != null ? query.from() : to.minus(Duration.ofHours(24));
        return readingRepository.findTop1000ByContenedorAndReceivedAtBetweenOrderByReceivedAtDesc(
                query.contenedor().trim(), from, to);
    }

    @Override
    public List<VaccineProfile> getVaccineProfiles() {
        return vaccineProfileRepository.findAll(Sort.by("name"));
    }

    private Set<AlertStatus> parseStatuses(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("OPEN")) {
            return AlertStatus.OPEN;
        }
        if (status.equalsIgnoreCase("ALL")) {
            return EnumSet.allOf(AlertStatus.class);
        }
        return EnumSet.of(AlertStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
    }
}
