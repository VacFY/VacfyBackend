package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lectura guardada de un termo")
public record ReadingResource(
        @Schema(example = "1520") Long id,
        @Schema(example = "001") String contenedor,
        @Schema(description = "°C", example = "5.4") Double temperatura,
        @Schema(description = "%; puede ser null", example = "61.0") Double humedad,
        @Schema(example = "2026-10-07T07:59:30Z") String receivedAt) { }
