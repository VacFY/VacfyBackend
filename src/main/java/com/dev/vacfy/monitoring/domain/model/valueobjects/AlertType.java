package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Reglas del motor de alertas. */
public enum AlertType {
    /** Temperatura fuera del rango del termo (calculado con sus lotes o su perfil). */
    OUT_OF_RANGE,
    /** Cambio brusco de temperatura en una ventana corta de tiempo. */
    RAPID_CHANGE,
    /** El contenedor dejó de enviar lecturas. */
    SENSOR_OFFLINE,
    /** El sensor envía lecturas nulas o físicamente imposibles. */
    INVALID_READING,
    /** Un lote vence pronto (WARNING; CRITICAL en los últimos días). */
    LOT_EXPIRING,
    /** Un lote venció: no usar. */
    LOT_EXPIRED;

    /** Alertas de un lote concreto (no las evalúa el motor de temperatura). */
    public boolean isLotRule() {
        return this == LOT_EXPIRING || this == LOT_EXPIRED;
    }
}
