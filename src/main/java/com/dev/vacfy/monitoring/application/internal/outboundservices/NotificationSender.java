package com.dev.vacfy.monitoring.application.internal.outboundservices;

/**
 * Puerto de salida: notificación push al celular de la enfermera.
 * Hoy la implementación no envía nada (NoOpNotificationSender). Para conectar Firebase Cloud Messaging,
 * basta con otra implementación de esta interfaz (ver README, "Notificaciones push").
 */
public interface NotificationSender {
    void send(PushNotification notification);
}
