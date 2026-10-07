package com.dev.vacfy.monitoring.domain.model.queries;

import java.time.Instant;

/** @param notBefore no devolver lecturas anteriores a esta fecha (desde cuándo la enfermera tiene el termo); null = sin límite */
public record GetReadingsQuery(String contenedor, Instant from, Instant to, Instant notBefore) {
    public GetReadingsQuery(String contenedor, Instant from, Instant to) {
        this(contenedor, from, to, null);
    }
}
