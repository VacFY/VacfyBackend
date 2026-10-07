package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Termo con su clave de vinculación. La clave solo aparece en esta respuesta: imprímala en la etiqueta del termo")
public record ContainerKeyResource(
        @Schema(example = "001") String codigo,
        @Schema(example = "Termo Posta Huambos") String nombre,
        @Schema(description = "Clave en claro, formato XXX-XXX. No se vuelve a mostrar; si se pierde, use regenerate-key",
                example = "K7P-29Q") String clave,
        @Schema(example = "true") boolean activo,
        @Schema(example = "2026-10-07T14:03:00Z") String creadoEn) { }
