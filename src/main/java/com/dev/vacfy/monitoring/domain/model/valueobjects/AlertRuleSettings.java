package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.time.Duration;

/** Umbrales del motor (vienen de application.yaml, vacty.alerts.*). */
public record AlertRuleSettings(
        int consecutiveReadings,
        double hysteresis,
        double rapidChangeDelta,
        Duration rapidChangeWindow,
        Duration offlineAfter,
        double physicalMin,
        double physicalMax) {

    public AlertRuleSettings {
        if (consecutiveReadings < 1) throw new IllegalArgumentException("consecutiveReadings >= 1");
        if (hysteresis < 0) throw new IllegalArgumentException("hysteresis >= 0");
        if (rapidChangeDelta <= 0) throw new IllegalArgumentException("rapidChangeDelta > 0");
    }

    public static AlertRuleSettings defaults() {
        return new AlertRuleSettings(3, 0.5, 2.0, Duration.ofMinutes(5), Duration.ofMinutes(2), -40.0, 80.0);
    }
}
