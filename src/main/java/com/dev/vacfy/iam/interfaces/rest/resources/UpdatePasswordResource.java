package com.dev.vacfy.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cambio de contraseña")
public record UpdatePasswordResource(
        @Schema(description = "Contraseña actual", example = "miClave123") String currentPassword,
        @Schema(description = "Contraseña nueva (no vacía, máximo 72 bytes)", example = "otraClave456") String newPassword) {
}
