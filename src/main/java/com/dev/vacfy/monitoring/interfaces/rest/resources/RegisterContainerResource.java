package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Termo nuevo")
public record RegisterContainerResource(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Código que envía el ESP32 (1 a 32 letras, números, - o _)",
                example = "001") String codigo,
        @Schema(description = "Nombre para reconocerlo (opcional, hasta 100 caracteres)", example = "Termo Posta Huambos") String nombre) { }
