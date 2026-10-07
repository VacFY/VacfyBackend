package com.dev.vacfy.user.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil del usuario. En una cuenta nueva, nombre, apellido y empresa valen \"Undefined\"")
public record ProfileResource(
        @Schema(example = "12345678") String profileDni,
        @Schema(example = "Ana") String profileName,
        @Schema(example = "Quispe") String profileLastName,
        @Schema(description = "Establecimiento de salud", example = "Posta Santa Rosa") String profileCompany,
        @Schema(description = "ENFERMERA ve solo sus termos; SUPERVISOR (microred) ve todos y administra los termos",
                allowableValues = {"ENFERMERA", "SUPERVISOR"}, example = "ENFERMERA") String role) {
}
