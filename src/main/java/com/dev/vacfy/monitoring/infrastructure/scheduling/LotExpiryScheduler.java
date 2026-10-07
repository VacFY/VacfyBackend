package com.dev.vacfy.monitoring.infrastructure.scheduling;

import com.dev.vacfy.monitoring.application.internal.commandservices.LotExpiryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Revisa los vencimientos de los lotes al arrancar y todos los días (por defecto a las 06:00, hora de la posta). */
@Component
public class LotExpiryScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LotExpiryScheduler.class);
    private final LotExpiryService lotExpiryService;

    public LotExpiryScheduler(LotExpiryService lotExpiryService) {
        this.lotExpiryService = lotExpiryService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        checkExpirations();
    }

    @Scheduled(cron = "${vacty.alerts.expiry-check-cron:0 0 6 * * *}", zone = "${vacty.timezone:America/Lima}")
    public void checkExpirations() {
        try {
            lotExpiryService.checkAll();
        } catch (Exception e) {
            LOGGER.error("Error revisando el vencimiento de los lotes", e);
        }
    }
}
