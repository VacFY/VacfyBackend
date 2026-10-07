package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil de rango (vista reducida de una vacuna del catálogo)")
public record VaccineProfileResource(
        @Schema(example = "1") Long id,
        @Schema(example = "PAI estándar 2–8 °C") String name,
        @Schema(example = "2.0") Double minTemp,
        @Schema(example = "8.0") Double maxTemp,
        @Schema(example = "true") Boolean freezeSensitive) { }
