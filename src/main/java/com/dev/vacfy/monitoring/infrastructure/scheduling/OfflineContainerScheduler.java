package com.dev.vacfy.monitoring.infrastructure.scheduling;

import com.dev.vacfy.monitoring.domain.services.MonitoringCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada 30 s revisa qué contenedores dejaron de enviar lecturas (regla SENSOR_OFFLINE). */
@Component
public class OfflineContainerScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfflineContainerScheduler.class);
    private final MonitoringCommandService monitoringCommandService;

    public OfflineContainerScheduler(MonitoringCommandService monitoringCommandService) {
        this.monitoringCommandService = monitoringCommandService;
    }

    @Scheduled(fixedDelayString = "${vacty.alerts.offline-check-ms:30000}", initialDelayString = "${vacty.alerts.offline-check-ms:30000}")
    public void checkOfflineContainers() {
        try {
            monitoringCommandService.checkOfflineContainers();
        } catch (Exception e) {
            LOGGER.error("Error revisando contenedores sin datos", e);
        }
    }
}
