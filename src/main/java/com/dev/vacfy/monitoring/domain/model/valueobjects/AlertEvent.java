package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Resultado del motor: abrir o cerrar una alerta de un tipo. */
public record AlertEvent(Action action, AlertType type, AlertSeverity severity, Double value, String message) {
    public enum Action { OPEN, RESOLVE }

    public static AlertEvent open(AlertType type, AlertSeverity severity, Double value, String message) {
        return new AlertEvent(Action.OPEN, type, severity, value, message);
    }

    public static AlertEvent resolve(AlertType type, Double value, String message) {
        return new AlertEvent(Action.RESOLVE, type, null, value, message);
    }

    public boolean isOpen() {
        return action == Action.OPEN;
    }
}
