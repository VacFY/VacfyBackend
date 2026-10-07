package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Corrección de una vacuna. Los campos que no envíes (o envíes en null) mantienen su valor actual")
public record UpdateVaccineResource(
        @Schema(example = "Pentavalente") String name,
        @Schema(example = "Difteria, tos ferina, tétanos, hepatitis B y Haemophilus influenzae tipo b (Hib)") String protectsAgainst,
        @Schema(example = "2.0") Double minTemp,
        @Schema(example = "8.0") Double maxTemp,
        @Schema(example = "true") Boolean freezeSensitive,
        @Schema(example = "false") Boolean heatSensitive,
        @Schema(example = "1") Integer dosesPerVial,
        String notes,
        @Schema(description = "Márcalo en true después de revisar la ficha técnica", example = "true") Boolean verified,
        @Schema(description = "Criterio de cuidado principal", allowableValues = {"ANTI_FREEZE", "PROTECT_FROM_LIGHT_AND_HEAT", "CHECK_MANUFACTURER"},
                example = "ANTI_FREEZE") String careProfile,
        @Schema(description = "Cuidados en el termo, uno por elemento. Reemplaza la lista completa",
                example = "[\"Se daña al congelarse.\", \"Usar paquetes fríos acondicionados.\", \"Evitar el contacto directo con el hielo.\"]")
        List<String> careInstructions) { }
