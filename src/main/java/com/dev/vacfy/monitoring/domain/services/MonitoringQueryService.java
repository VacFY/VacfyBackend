package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.aggregates.Reading;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.queries.GetAlertsQuery;
import com.dev.vacfy.monitoring.domain.model.queries.GetReadingsQuery;

import java.util.List;
import java.util.Optional;

public interface MonitoringQueryService {
    List<Alert> handle(GetAlertsQuery query);

    Optional<Alert> getAlert(Long alertId);

    List<Reading> handle(GetReadingsQuery query);

    List<VaccineProfile> getVaccineProfiles();
}
