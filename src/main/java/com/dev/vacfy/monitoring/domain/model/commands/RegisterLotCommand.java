package com.dev.vacfy.monitoring.domain.model.commands;

/**
 * @param expiryDate AAAA-MM-DD
 * @param source     SCAN, TYPED_CODE o MANUAL (por defecto MANUAL)
 */
public record RegisterLotCommand(String contenedor, Long vaccineId, String lotNumber, String expiryDate, Integer vials,
                                 Integer doses, String gtin, String source, String registeredBy) { }
