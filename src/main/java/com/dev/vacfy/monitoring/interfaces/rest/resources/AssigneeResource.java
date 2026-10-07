package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persona que tiene o tuvo el termo")
public record AssigneeResource(
        @Schema(example = "d5477e77-eab2-499c-bf91-6817865ac156") String userId,
        @Schema(example = "12345678") String dni,
        @Schema(description = "Nombre y apellido; null si no completó su perfil", example = "Ana Quispe") String nombre) { }
