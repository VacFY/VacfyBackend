package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Código del termo y su clave (la de la etiqueta, o el código temporal del QR)")
public record LinkContainerResource(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "001") String codigo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Se ignoran mayúsculas, espacios y el guion",
                example = "K7P-29Q") String clave) { }
