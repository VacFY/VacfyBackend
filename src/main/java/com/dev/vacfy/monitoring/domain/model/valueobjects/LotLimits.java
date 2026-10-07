package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Un lote activo con el rango de conservación de su vacuna. */
public record LotLimits(LotRef lot, double minTemp, double maxTemp, boolean freezeSensitive, boolean heatSensitive) {
    public boolean isTooCold(double temperature) {
        return temperature < minTemp;
    }

    public boolean isTooHot(double temperature) {
        return temperature > maxTemp;
    }
}
