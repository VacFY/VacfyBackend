package com.dev.vacfy.monitoring.interfaces.rest.resources;

/** Las fechas van como texto ISO-8601 en UTC (p. ej. 2026-10-07T14:03:00Z). */
public record AlertResource(
        Long id,
        String contenedor,
        String type,
        String severity,
        String status,
        String message,
        Double triggerValue,
        Double minValue,
        Double maxValue,
        String startedAt,
        String acknowledgedAt,
        String acknowledgedBy,
        String resolvedAt,
        String resolutionMessage) { }
