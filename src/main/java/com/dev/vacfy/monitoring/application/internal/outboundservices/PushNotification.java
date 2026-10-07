package com.dev.vacfy.monitoring.application.internal.outboundservices;

/**
 * Lo que se mandaría al celular cuando se abre una alerta o sube a CRITICAL.
 *
 * @param title título corto de la notificación
 * @param body  mensaje para la enfermera
 */
public record PushNotification(Long alertId, String contenedor, String type, String severity, String title, String body) { }
