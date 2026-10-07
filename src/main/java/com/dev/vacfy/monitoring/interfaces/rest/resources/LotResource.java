package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lote de un termo con su vacuna")
public record LotResource(
        @Schema(example = "7") Long id,
        @Schema(example = "001") String contenedor,
        @Schema(description = "Vacuna del lote: nombre, para qué sirve y rango") VaccineResource vaccine,
        @Schema(example = "08901234567890") String gtin,
        @Schema(example = "AB1234") String lotNumber,
        @Schema(description = "AAAA-MM-DD", example = "2027-12-31") String expiryDate,
        @Schema(description = "Días para vencer según la fecha de la posta; negativo si ya venció", example = "450") long daysToExpiry,
        @Schema(example = "20") Integer vials,
        @Schema(example = "200") Integer doses,
        @Schema(allowableValues = {"SCAN", "TYPED_CODE", "MANUAL"}, example = "SCAN") String source,
        @Schema(allowableValues = {"ACTIVE", "USED", "DISCARDED", "EXPIRED"}, example = "ACTIVE") String status,
        @Schema(description = "Id del usuario que lo registró") String registeredBy,
        @Schema(example = "2026-10-07T14:03:00Z") String registeredAt,
        @Schema(description = "Cuándo se cerró (null si sigue en el termo)") String closedAt,
        @Schema(description = "Motivo del cierre") String closeReason) { }
