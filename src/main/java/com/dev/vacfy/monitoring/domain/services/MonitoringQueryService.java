package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.aggregates.Reading;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.queries.GetAlertsQuery;
import com.dev.vacfy.monitoring.domain.model.queries.GetReadingsQuery;

import java.util.List;

public interface MonitoringQueryService {
    List<Alert> handle(GetAlertsQuery query);

    List<Reading> handle(GetReadingsQuery query);

    List<VaccineProfile> getVaccineProfiles();
}
