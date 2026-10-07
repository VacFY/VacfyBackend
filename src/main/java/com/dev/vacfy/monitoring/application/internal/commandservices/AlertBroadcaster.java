package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.application.internal.outboundservices.AlertPublisher;
import com.dev.vacfy.monitoring.application.internal.outboundservices.NotificationSender;
import com.dev.vacfy.monitoring.application.internal.outboundservices.PushNotification;
import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.services.AlertTexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Avisa a los clientes de cada cambio de una alerta: siempre por /ws/alerts y, cuando se abre o sube
 * a CRITICAL, también por notificación push.
 */
@Component
public class AlertBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertBroadcaster.class);
    private final AlertPublisher alertPublisher;
    private final NotificationSender notificationSender;

    public AlertBroadcaster(AlertPublisher alertPublisher, NotificationSender notificationSender) {
        this.alertPublisher = alertPublisher;
        this.notificationSender = notificationSender;
    }

    /** Alerta nueva: WebSocket + push. */
    public void opened(Alert alert) {
        alertPublisher.publish(alert);
        push(alert);
    }

    /** Alerta que subió de severidad: WebSocket + push. */
    public void escalated(Alert alert) {
        alertPublisher.publish(alert);
        push(alert);
    }

    /** Vista o cerrada: solo WebSocket. */
    public void updated(Alert alert) {
        alertPublisher.publish(alert);
    }

    private void push(Alert alert) {
        try {
            String title = alert.getTitle() != null ? alert.getTitle() : AlertTexts.fallbackTitle(alert.getType(), alert.getContenedor());
            notificationSender.send(new PushNotification(alert.getId(), alert.getContenedor(), alert.getType().name(),
                    alert.getSeverity().name(), title, alert.getMessage()));
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar la notificación de la alerta {}", alert.getId(), e);
        }
    }
}
