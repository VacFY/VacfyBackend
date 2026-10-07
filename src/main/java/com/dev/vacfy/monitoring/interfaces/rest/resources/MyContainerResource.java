package com.dev.vacfy.monitoring.interfaces.rest.resources;

import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource.NextExpiryResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource.RangeResource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Termo de la enfermera, con los mismos datos que el dashboard")
public record MyContainerResource(
        @Schema(example = "001") String contenedor,
        @Schema(example = "Termo Posta Huambos") String nombre,
        @Schema(description = "Desde cuándo lo tiene; el historial de lecturas empieza aquí", example = "2026-10-07T07:00:00Z") String asignadoDesde,
        @Schema(allowableValues = {"OK", "ALERTA", "SIN_DATOS"}, example = "OK") String status,
        @Schema(example = "5.4") Double temperatura,
        @Schema(example = "61.0") Double humedad,
        @Schema(example = "2026-10-07T14:04:58Z") String lastReadingAt,
        RangeResource range,
        @Schema(example = "3") int activeLots,
        @Schema(example = "0") int expiredLots,
        NextExpiryResource nextExpiry,
        @Schema(example = "0") int openAlerts,
        @Schema(allowableValues = {"WARNING", "CRITICAL"}) String highestSeverity) { }
