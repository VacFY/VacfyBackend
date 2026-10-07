package com.dev.vacfy.monitoring.domain.model.valueobjects;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;

/** Lote con su vacuna y los días que le quedan (calculados con la fecha local de la posta). */
public record LotView(VaccineLot lot, VaccineProfile vaccine, long daysToExpiry) { }
