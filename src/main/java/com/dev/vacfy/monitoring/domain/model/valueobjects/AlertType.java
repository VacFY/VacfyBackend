package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Reglas del motor de alertas. */
public enum AlertType {
    /** Temperatura fuera del rango del perfil de la vacuna. */
    OUT_OF_RANGE,
    /** Cambio brusco de temperatura en una ventana corta de tiempo. */
    RAPID_CHANGE,
    /** El contenedor dejó de enviar lecturas. */
    SENSOR_OFFLINE,
    /** El sensor envía lecturas nulas o físicamente imposibles. */
    INVALID_READING
}
