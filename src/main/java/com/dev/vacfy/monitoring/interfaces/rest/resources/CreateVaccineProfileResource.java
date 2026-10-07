package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nuevo perfil de rango")
public record CreateVaccineProfileResource(
        @Schema(example = "Congelado -25 a -15") String name,
        @Schema(example = "-25.0") Double minTemp,
        @Schema(example = "-15.0") Double maxTemp,
        @Schema(description = "Por defecto false", example = "false") Boolean freezeSensitive) { }
