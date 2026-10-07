package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Alerta tal como la reciben web y móvil, por REST y por /ws/alerts.
 * Las fechas van como texto ISO-8601 en UTC (p. ej. 2026-10-07T14:03:00Z).
 */
@Schema(description = "Alerta generada por el backend. Web y móvil solo la muestran: título, mensaje y severidad ya vienen listos")
public record AlertResource(
        @Schema(example = "42") Long id,
        @Schema(description = "Termo", example = "001") String contenedor,
        @Schema(allowableValues = {"OUT_OF_RANGE", "RAPID_CHANGE", "SENSOR_OFFLINE", "INVALID_READING", "LOT_EXPIRING", "LOT_EXPIRED"},
                example = "OUT_OF_RANGE") String type,
        @Schema(allowableValues = {"WARNING", "CRITICAL"}, example = "CRITICAL") String severity,
        @Schema(description = "ACTIVE → ACKNOWLEDGED → RESOLVED", allowableValues = {"ACTIVE", "ACKNOWLEDGED", "RESOLVED"},
                example = "ACTIVE") String status,
        @Schema(description = "Texto para la enfermera",
                example = "1,2 °C: riesgo de congelación para Pentavalente (lote AB123) y Hepatitis B (lote HB77). Rango del termo: 2,0 – 8,0 °C. No use las vacunas hasta evaluarlas.")
        String message,
        @Schema(description = "Temperatura que abrió la alerta (null en SENSOR_OFFLINE y en las de lotes)", example = "1.2") Double triggerValue,
        @Schema(description = "Temperatura mínima registrada mientras estuvo abierta", example = "0.8") Double minValue,
        @Schema(description = "Temperatura máxima registrada mientras estuvo abierta", example = "1.4") Double maxValue,
        @Schema(example = "2026-10-07T14:03:00Z") String startedAt,
        @Schema(description = "Cuándo se marcó como vista") String acknowledgedAt,
        @Schema(description = "Id del usuario que la marcó como vista") String acknowledgedBy,
        @Schema(description = "Cuándo se cerró") String resolvedAt,
        @Schema(description = "Texto de cierre", example = "Temperatura de vuelta en rango: 4,5 °C.") String resolutionMessage,
        @Schema(description = "Título corto, para la notificación del celular", example = "Riesgo de congelación en termo 001")
        String title,
        @Schema(description = "Lotes en riesgo (vacía si la alerta no es de lotes concretos)") List<AffectedLotResource> affectedLots,
        @Schema(description = "Lote de una alerta de vencimiento; úsalo con PATCH /api/v1/lots/{lotId}/close", example = "7")
        Long lotId) { }
