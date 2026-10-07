package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Código de la caja tal como llega del escáner o como lo escribió la enfermera")
public record ReadCodeResource(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Texto crudo del escáner (con o sin \"]d2\" y con el separador GS = \\u001d), "
                        + "formato legible \"(01)…(17)…(10)…\" o solo el GTIN",
                example = "(01)08901234567890(17)271231(10)AB1234")
        String code) { }
