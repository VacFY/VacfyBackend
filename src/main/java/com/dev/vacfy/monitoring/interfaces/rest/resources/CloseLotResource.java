package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cierre de un lote")
public record CloseLotResource(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"USED", "DISCARDED"}, example = "DISCARDED") String status,
        @Schema(description = "Motivo (opcional, hasta 300 caracteres)", example = "Vencido") String reason) { }
