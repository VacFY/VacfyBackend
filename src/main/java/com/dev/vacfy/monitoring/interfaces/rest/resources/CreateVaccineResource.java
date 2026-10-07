package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Nueva vacuna. Solo el nombre es obligatorio; el rango por defecto es 2–8 °C")
public record CreateVaccineResource(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Fiebre amarilla") String name,
        @Schema(example = "Fiebre amarilla") String protectsAgainst,
        @Schema(description = "Por defecto 2.0", example = "2.0") Double minTemp,
        @Schema(description = "Por defecto 8.0", example = "8.0") Double maxTemp,
        @Schema(description = "Por defecto false", example = "false") Boolean freezeSensitive,
        @Schema(description = "Por defecto false", example = "true") Boolean heatSensitive,
        @Schema(example = "10") Integer dosesPerVial,
        @Schema(example = "Reconstituir con su diluyente") String notes,
        @Schema(description = "Criterio de cuidado principal", allowableValues = {"ANTI_FREEZE", "PROTECT_FROM_LIGHT_AND_HEAT", "CHECK_MANUFACTURER"},
                example = "ANTI_FREEZE") String careProfile,
        @Schema(description = "Cuidados en el termo, uno por elemento",
                example = "[\"Se daña al congelarse.\", \"Usar paquetes fríos acondicionados.\", \"Evitar el contacto directo con el hielo.\"]")
        List<String> careInstructions) { }
