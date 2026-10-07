package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertEvent;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertSeverity;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotRef;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de vencimiento de lotes. Java puro: recibe el lote, la fecha de hoy y qué alertas tiene
 * abiertas, y devuelve qué abrir, escalar o cerrar. Una sola alerta activa por lote y tipo.
 *
 *  - LOT_EXPIRING: vence en ≤ expiringDays → WARNING; en ≤ criticalDays → CRITICAL (escala la misma alerta).
 *  - LOT_EXPIRED: ya venció → CRITICAL; cierra la de "por vencer" y el lote pasa a EXPIRED.
 * Un lote vence al terminar el día de su fecha de vencimiento.
 */
public final class LotExpiryRules {
    public enum Stage { OK, EXPIRING, EXPIRING_SOON, EXPIRED }

    private final int expiringDays;
    private final int criticalDays;

    public LotExpiryRules(int expiringDays, int criticalDays) {
        if (criticalDays < 0 || expiringDays < criticalDays) {
            throw new IllegalArgumentException("Se requiere 0 ≤ expiring-critical-days ≤ expiring-days");
        }
        this.expiringDays = expiringDays;
        this.criticalDays = criticalDays;
    }

    public int getExpiringDays() {
        return expiringDays;
    }

    public Stage stage(LocalDate expiry, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, expiry);
        if (days < 0) return Stage.EXPIRED;
        if (days <= criticalDays) return Stage.EXPIRING_SOON;
        if (days <= expiringDays) return Stage.EXPIRING;
        return Stage.OK;
    }

    /**
     * @param openExpiring severidad de la alerta LOT_EXPIRING abierta del lote, o null si no tiene
     * @param expiredOpen  true si el lote ya tiene una alerta LOT_EXPIRED abierta
     */
    public List<AlertEvent> evaluate(LotRef lot, LocalDate today, AlertSeverity openExpiring, boolean expiredOpen) {
        List<AlertEvent> events = new ArrayList<>();
        switch (stage(lot.expiryDate(), today)) {
            case OK -> { }
            case EXPIRING -> {
                if (openExpiring == null) events.add(expiring(lot, AlertSeverity.WARNING));
            }
            case EXPIRING_SOON -> {
                if (openExpiring == null) {
                    events.add(expiring(lot, AlertSeverity.CRITICAL));
                } else if (openExpiring == AlertSeverity.WARNING) {
                    events.add(AlertEvent.escalate(AlertType.LOT_EXPIRING, AlertSeverity.CRITICAL,
                            AlertTexts.lotExpiringTitle(lot), expiringMessage(lot, AlertSeverity.CRITICAL)));
                }
            }
            case EXPIRED -> {
                if (openExpiring != null) {
                    events.add(AlertEvent.resolve(AlertType.LOT_EXPIRING, null,
                            "El lote venció el " + AlertTexts.date(lot.expiryDate()) + "."));
                }
                if (!expiredOpen) {
                    events.add(AlertEvent.open(AlertType.LOT_EXPIRED, AlertSeverity.CRITICAL, null,
                            AlertTexts.lotExpiredTitle(lot), expiredMessage(lot), List.of(lot.toAffected())));
                }
            }
        }
        return events;
    }

    /** "No usar: lote X de <vacuna> venció el dd/mm/aaaa" + qué hacer. */
    public static String expiredMessage(LotRef lot) {
        return "No usar: lote " + lot.lotNumber() + " de " + lot.vaccine() + " venció el " + AlertTexts.date(lot.expiryDate())
                + ". Retírelo del termo " + lot.contenedor() + " y regístrelo como descartado.";
    }

    private AlertEvent expiring(LotRef lot, AlertSeverity severity) {
        return AlertEvent.open(AlertType.LOT_EXPIRING, severity, null, AlertTexts.lotExpiringTitle(lot),
                expiringMessage(lot, severity), List.of(lot.toAffected()));
    }

    private String expiringMessage(LotRef lot, AlertSeverity severity) {
        String base = "El lote " + lot.lotNumber() + " de " + lot.vaccine() + " (termo " + lot.contenedor()
                + ") vence el " + AlertTexts.date(lot.expiryDate());
        return severity == AlertSeverity.CRITICAL
                ? base + ", en " + criticalDays + " días o menos. Úselo primero o coordine su reemplazo."
                : base + ". Úselo antes que los lotes que vencen después.";
    }
}
