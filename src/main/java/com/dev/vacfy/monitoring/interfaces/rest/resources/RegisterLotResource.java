package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lote que entra a un termo")
public record RegisterLotResource(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Código del termo (el que envía el ESP32)", example = "001") String contenedor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Id de GET /api/v1/vaccines", example = "1") Long vaccineId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Hasta 20 caracteres; se guarda en mayúsculas", example = "AB1234") String lotNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "AAAA-MM-DD. No puede ser anterior a hoy", example = "2027-12-31") String expiryDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Número de frascos (1 o más)", example = "20") Integer vials,
        @Schema(description = "Número de dosis (opcional)", example = "200") Integer doses,
        @Schema(description = "GTIN (opcional). Si es nuevo, queda asociado a la vacuna para la próxima vez", example = "08901234567890") String gtin,
        @Schema(description = "Cómo se obtuvieron los datos. Por defecto MANUAL",
                allowableValues = {"SCAN", "TYPED_CODE", "MANUAL"}, example = "SCAN") String source) { }
