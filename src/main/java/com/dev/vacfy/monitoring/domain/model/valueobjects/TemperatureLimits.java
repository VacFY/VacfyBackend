package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.util.List;

/**
 * Rango permitido para un contenedor. Sale de sus lotes activos (lots no vacío)
 * o, si no tiene lotes, de su perfil de vacuna.
 */
public record TemperatureLimits(String profileName, double minTemp, double maxTemp,
                                boolean freezeSensitive, boolean heatSensitive, List<LotLimits> lots) {
    public TemperatureLimits {
        if (Double.isNaN(minTemp) || Double.isNaN(maxTemp) || minTemp >= maxTemp) {
            throw new IllegalArgumentException("Rango de temperatura inválido: " + minTemp + " – " + maxTemp);
        }
        lots = lots == null ? List.of() : List.copyOf(lots);
    }

    public TemperatureLimits(String profileName, double minTemp, double maxTemp, boolean freezeSensitive) {
        this(profileName, minTemp, maxTemp, freezeSensitive, false, List.of());
    }

    public boolean isOutOfRange(double temperature) {
        return temperature < minTemp || temperature > maxTemp;
    }

    public boolean basedOnLots() {
        return !lots.isEmpty();
    }
}
