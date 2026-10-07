package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pantalla de inicio de web y móvil: un resumen por termo")
public record DashboardSummaryResource(
        @Schema(description = "Momento del cálculo", example = "2026-10-07T14:05:00Z") String generatedAt,
        @Schema(description = "Termos ordenados por código") List<ContainerSummaryResource> containers) {

    @Schema(description = "Resumen de un termo")
    public record ContainerSummaryResource(
            @Schema(example = "001") String contenedor,
            @Schema(description = "OK: envía datos y sin alertas · ALERTA: alguna alerta abierta · SIN_DATOS: nunca envió o lleva más de 2 min sin enviar",
                    allowableValues = {"OK", "ALERTA", "SIN_DATOS"}, example = "ALERTA") String status,
            @Schema(description = "Última temperatura válida (°C)", example = "5.4") Double temperatura,
            @Schema(description = "Última humedad (%)", example = "61.0") Double humedad,
            @Schema(description = "Cuándo llegó esa lectura", example = "2026-10-07T14:04:58Z") String lastReadingAt,
            @Schema(description = "Rango que se está vigilando") RangeResource range,
            @Schema(description = "Lotes activos en el termo", example = "3") int activeLots,
            @Schema(description = "Lotes vencidos que siguen en el termo (hay que descartarlos)", example = "0") int expiredLots,
            @Schema(description = "Lote activo que vence primero (null si no hay lotes)") NextExpiryResource nextExpiry,
            @Schema(description = "Alertas abiertas (ACTIVE o ACKNOWLEDGED)", example = "1") int openAlerts,
            @Schema(description = "Severidad más alta de las alertas abiertas (null si no hay)",
                    allowableValues = {"WARNING", "CRITICAL"}, example = "WARNING") String highestSeverity) { }

    @Schema(description = "Rango de temperatura del termo")
    public record RangeResource(
            @Schema(example = "2.0") double minTemp,
            @Schema(example = "8.0") double maxTemp,
            @Schema(description = "LOTS: calculado con los lotes activos · PROFILE: perfil asignado o el de por defecto",
                    allowableValues = {"LOTS", "PROFILE"}, example = "LOTS") String basedOn,
            @Schema(description = "Nombre del perfil cuando basedOn = PROFILE", example = "PAI estándar 2–8 °C") String profileName,
            @Schema(description = "Algún lote (o el perfil) se daña si se congela", example = "true") boolean freezeSensitive,
            @Schema(description = "Algún lote (o el perfil) pierde efecto con el calor", example = "false") boolean heatSensitive) { }

    @Schema(description = "Próximo vencimiento del termo")
    public record NextExpiryResource(
            @Schema(example = "7") Long lotId,
            @Schema(example = "Pentavalente") String vaccine,
            @Schema(example = "AB1234") String lotNumber,
            @Schema(example = "2026-11-06") String expiryDate,
            @Schema(example = "30") long daysToExpiry) { }
}
