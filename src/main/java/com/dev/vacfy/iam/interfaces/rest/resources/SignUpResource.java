package com.dev.vacfy.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos para crear una cuenta")
public record SignUpResource(
        @Schema(description = "DNI; debe ser único", example = "12345678") String userDni,
        @Schema(description = "Contraseña (no vacía, máximo 72 bytes)", example = "miClave123") String userPassword) {
}
