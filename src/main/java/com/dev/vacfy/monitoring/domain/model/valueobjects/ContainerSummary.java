package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.time.Instant;

/**
 * Resumen de un termo para la pantalla de inicio.
 *
 * @param nextExpiry lote ACTIVE que vence primero, o null si no tiene lotes
 */
public record ContainerSummary(String contenedor, ContainerStatus status, Double temperatura, Double humedad,
                               Instant lastReadingAt, TemperatureLimits limits, int activeLots, int expiredLots,
                               LotView nextExpiry, int openAlerts, AlertSeverity highestSeverity) { }
