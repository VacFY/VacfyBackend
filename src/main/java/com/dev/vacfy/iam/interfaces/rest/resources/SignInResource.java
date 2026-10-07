package com.dev.vacfy.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credenciales para iniciar sesión")
public record SignInResource(
        @Schema(description = "DNI del usuario", example = "12345678") String userDni,
        @Schema(description = "Contraseña", example = "miClave123") String userPassword) {
}
