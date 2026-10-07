package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Lo que trae un código GS1 de una caja de vacunas. Cada campo puede venir en null.
 *
 * @param gtin   AI 01, 14 dígitos con el dígito verificador ya validado
 * @param expiry AI 17, vencimiento
 * @param lot    AI 10, número de lote
 * @param serial AI 21, número de serie
 */
public record Gs1Data(String gtin, LocalDate expiry, String lot, String serial) { }
