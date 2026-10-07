package com.dev.vacfy.monitoring.infrastructure.config;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertRuleSettings;
import com.dev.vacfy.monitoring.domain.services.AlertRuleEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

@Configuration
@EnableScheduling
public class MonitoringConfiguration {

    @Bean
    public AlertRuleEngine alertRuleEngine(
            @Value("${vacty.alerts.consecutive-readings:3}") int consecutiveReadings,
            @Value("${vacty.alerts.hysteresis:0.5}") double hysteresis,
            @Value("${vacty.alerts.rapid-change-delta:2.0}") double rapidChangeDelta,
            @Value("${vacty.alerts.rapid-change-window-seconds:300}") long rapidChangeWindowSeconds,
            @Value("${vacty.alerts.offline-after-seconds:120}") long offlineAfterSeconds,
            @Value("${vacty.alerts.physical-min:-40.0}") double physicalMin,
            @Value("${vacty.alerts.physical-max:80.0}") double physicalMax) {
        return new AlertRuleEngine(new AlertRuleSettings(
                consecutiveReadings,
                hysteresis,
                rapidChangeDelta,
                Duration.ofSeconds(rapidChangeWindowSeconds),
                Duration.ofSeconds(offlineAfterSeconds),
                physicalMin,
                physicalMax));
    }
}
