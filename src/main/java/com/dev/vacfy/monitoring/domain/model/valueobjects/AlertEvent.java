package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.util.List;

/**
 * Resultado de una regla: abrir, cerrar o escalar (subir de severidad) una alerta de un tipo.
 *
 * @param title        título corto para la notificación (puede ser null al cerrar)
 * @param affectedLots lotes en riesgo (vacío si la alerta no es de lotes concretos)
 */
public record AlertEvent(Action action, AlertType type, AlertSeverity severity, Double value, String message,
                         String title, List<AffectedLot> affectedLots) {
    public enum Action { OPEN, RESOLVE, ESCALATE }

    public AlertEvent {
        affectedLots = affectedLots == null ? List.of() : List.copyOf(affectedLots);
    }

    public static AlertEvent open(AlertType type, AlertSeverity severity, Double value, String title, String message,
                                  List<AffectedLot> affectedLots) {
        return new AlertEvent(Action.OPEN, type, severity, value, message, title, affectedLots);
    }

    public static AlertEvent resolve(AlertType type, Double value, String message) {
        return new AlertEvent(Action.RESOLVE, type, null, value, message, null, List.of());
    }

    public static AlertEvent escalate(AlertType type, AlertSeverity severity, String title, String message) {
        return new AlertEvent(Action.ESCALATE, type, severity, null, message, title, List.of());
    }

    public boolean isOpen() {
        return action == Action.OPEN;
    }
}
