package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de vincular un termo")
public record LinkResultResource(
        @Schema(example = "001") String contenedor,
        @Schema(example = "Termo Posta Huambos") String nombre,
        @Schema(description = "Desde cuándo lo tiene", example = "2026-10-07T14:03:00Z") String asignadoDesde,
        @Schema(description = "false si el termo ya era suyo (no se duplicó la asignación)", example = "true") boolean nuevaAsignacion) { }
