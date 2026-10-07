package com.dev.vacfy.monitoring.interfaces.rest.resources;

import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource.NextExpiryResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource.RangeResource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Termo en la vista del supervisor: registro, quién lo tiene y su estado")
public record ContainerOverviewResource(
        @Schema(example = "001") String contenedor,
        @Schema(example = "Termo Posta Huambos") String nombre,
        @Schema(description = "false si envía datos pero nadie lo registró (no se puede vincular hasta registrarlo)", example = "true")
        boolean registrado,
        @Schema(example = "true") boolean activo,
        @Schema(description = "Quién lo tiene ahora (null si nadie)") AssignedToResource asignadoA,
        @Schema(allowableValues = {"OK", "ALERTA", "SIN_DATOS"}, example = "OK") String status,
        @Schema(example = "5.4") Double temperatura,
        @Schema(example = "61.0") Double humedad,
        @Schema(example = "2026-10-07T14:04:58Z") String lastReadingAt,
        RangeResource range,
        @Schema(example = "3") int activeLots,
        @Schema(example = "0") int expiredLots,
        NextExpiryResource nextExpiry,
        @Schema(example = "0") int openAlerts,
        @Schema(allowableValues = {"WARNING", "CRITICAL"}) String highestSeverity) {

    @Schema(description = "Asignación activa")
    public record AssignedToResource(
            @Schema(example = "d5477e77-eab2-499c-bf91-6817865ac156") String userId,
            @Schema(example = "12345678") String dni,
            @Schema(example = "Ana Quispe") String nombre,
            @Schema(example = "2026-10-07T07:00:00Z") String desde) { }
}
