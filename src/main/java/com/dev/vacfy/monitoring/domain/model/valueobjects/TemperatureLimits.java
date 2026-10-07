package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Rango permitido para un contenedor, tomado de su perfil de vacuna. */
public record TemperatureLimits(String profileName, double minTemp, double maxTemp, boolean freezeSensitive) {
    public TemperatureLimits {
        if (Double.isNaN(minTemp) || Double.isNaN(maxTemp) || minTemp >= maxTemp) {
            throw new IllegalArgumentException("Rango de temperatura inválido: " + minTemp + " – " + maxTemp);
        }
    }

    public boolean isOutOfRange(double temperature) {
        return temperature < minTemp || temperature > maxTemp;
    }
}
