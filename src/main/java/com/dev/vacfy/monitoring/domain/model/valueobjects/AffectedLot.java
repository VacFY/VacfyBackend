package com.dev.vacfy.monitoring.domain.model.valueobjects;

/**
 * Lote afectado por una alerta, tal como lo reciben web y móvil.
 * expiryDate va como texto ISO (2027-12-31) para serializarse igual por REST y por WebSocket.
 */
public record AffectedLot(Long lotId, String vaccine, String lotNumber, String expiryDate) { }
