package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error con un mensaje listo para mostrar")
public record ApiErrorResource(
        @Schema(example = "2026-10-07T14:03:00Z") String timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "Conflict") String error,
        @Schema(description = "Motivo, en español, para mostrar a la enfermera",
                example = "No se puede guardar SPR en el termo 001: necesita 2,0 a 8,0 °C y no tiene un rango común con Varicela (lote VZ9, -50,0 a -15,0 °C).")
        String message,
        @Schema(example = "/api/v1/lots") String path) { }
