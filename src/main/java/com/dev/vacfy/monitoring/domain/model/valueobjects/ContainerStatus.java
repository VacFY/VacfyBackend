package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Estado de un termo en la pantalla de inicio. */
public enum ContainerStatus {
    /** Envía datos y no tiene alertas abiertas. */
    OK,
    /** Envía datos y tiene al menos una alerta abierta (temperatura o vencimiento). */
    ALERTA,
    /** Nunca envió datos o lleva más de 2 min sin enviar. */
    SIN_DATOS
}
