package com.dev.vacfy.user.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos del perfil (los tres obligatorios)")
public record UpdateProfileResource(
        @Schema(example = "Ana") String profileName,
        @Schema(example = "Quispe") String profileLastName,
        @Schema(example = "Posta Santa Rosa") String profileCompany) {
}
