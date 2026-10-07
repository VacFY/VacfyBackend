package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lote en riesgo dentro de una alerta")
public record AffectedLotResource(
        @Schema(description = "Id del lote; úsalo con PATCH /api/v1/lots/{lotId}/close", example = "7") Long lotId,
        @Schema(example = "Pentavalente") String vaccine,
        @Schema(example = "AB123") String lotNumber,
        @Schema(description = "AAAA-MM-DD", example = "2027-12-31") String expiryDate) { }
