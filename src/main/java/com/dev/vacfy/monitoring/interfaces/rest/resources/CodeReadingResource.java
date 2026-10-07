package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lo que se leyó del código. No se guarda nada: con esto se llena el formulario de registro")
public record CodeReadingResource(
        @Schema(description = "GTIN de 14 dígitos (null si el código no lo trae)", example = "08901234567890") String gtin,
        @Schema(description = "Número de lote (AI 10)", example = "AB1234") String lotNumber,
        @Schema(description = "Vencimiento (AI 17), AAAA-MM-DD", example = "2027-12-31") String expiryDate,
        @Schema(description = "Número de serie (AI 21)", example = "SN0001") String serial,
        @Schema(description = "Días hasta el vencimiento; negativo si ya venció", example = "450") Long daysToExpiry,
        @Schema(description = "Vacuna asociada al GTIN, o null si el GTIN no está registrado") VaccineResource vaccine,
        @Schema(description = "true si el GTIN ya estaba en el catálogo", example = "true") boolean knownProduct,
        @Schema(description = "Avisos para mostrar antes de registrar",
                example = "[\"Vence en 12 días.\", \"GTIN no registrado: elija la vacuna.\"]") List<String> warnings) { }
