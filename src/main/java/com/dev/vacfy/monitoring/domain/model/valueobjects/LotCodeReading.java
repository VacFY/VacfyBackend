package com.dev.vacfy.monitoring.domain.model.valueobjects;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;

import java.util.List;

/**
 * Resultado de leer un código antes de registrar el lote (no se guarda nada).
 *
 * @param vaccine      vacuna asociada al GTIN, o null si el GTIN no está en el catálogo
 * @param daysToExpiry días hasta el vencimiento (negativo si ya venció), o null si el código no trae fecha
 * @param warnings     avisos listos para mostrar
 */
public record LotCodeReading(Gs1Data data, VaccineProfile vaccine, boolean knownProduct, Long daysToExpiry,
                             List<String> warnings) { }
