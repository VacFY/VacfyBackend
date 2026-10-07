package com.dev.vacfy.monitoring.domain.model.valueobjects;

import com.dev.vacfy.monitoring.domain.model.aggregates.Container;

import java.time.Instant;

/** Termo de la enfermera con su resumen del dashboard. */
public record MyContainer(Container container, ContainerSummary summary, Instant asignadoDesde) { }
