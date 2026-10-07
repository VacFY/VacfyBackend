package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vacuna del catálogo: para qué sirve y cómo se conserva")
public record VaccineResource(
        @Schema(example = "1") Long id,
        @Schema(example = "Pentavalente") String name,
        @Schema(description = "Para qué sirve, en palabras de la enfermera",
                example = "Difteria, tos ferina, tétanos, hepatitis B y Haemophilus influenzae tipo b (Hib)") String protectsAgainst,
        @Schema(description = "Temperatura mínima de conservación (°C)", example = "2.0") Double minTemp,
        @Schema(description = "Temperatura máxima de conservación (°C)", example = "8.0") Double maxTemp,
        @Schema(description = "Se daña si se congela", example = "true") Boolean freezeSensitive,
        @Schema(description = "Pierde efecto con el calor", example = "false") Boolean heatSensitive,
        @Schema(description = "Dosis por frasco (opcional)", example = "10") Integer dosesPerVial,
        @Schema(description = "Notas libres (opcional)") String notes,
        @Schema(description = "true cuando alguien revisó los datos con la ficha técnica del fabricante", example = "false") Boolean verified) { }
