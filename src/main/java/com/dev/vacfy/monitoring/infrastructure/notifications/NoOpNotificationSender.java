package com.dev.vacfy.monitoring.infrastructure.notifications;

import com.dev.vacfy.monitoring.application.internal.outboundservices.NotificationSender;
import com.dev.vacfy.monitoring.application.internal.outboundservices.PushNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implementación vacía: deja constancia en el log y no envía nada. Reemplazar por la de Firebase Cloud Messaging. */
@Component
public class NoOpNotificationSender implements NotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpNotificationSender.class);

    @Override
    public void send(PushNotification notification) {
        LOGGER.debug("Push no enviado (sin proveedor configurado): [{}] {}", notification.severity(), notification.title());
    }
}
