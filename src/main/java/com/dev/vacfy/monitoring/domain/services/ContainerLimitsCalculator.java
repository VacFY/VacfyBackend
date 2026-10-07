package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.LotLimits;
import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;

import java.util.List;

/**
 * Rango de un termo según sus lotes activos. Java puro.
 *
 *  - mínimo = el mayor de los mínimos; máximo = el menor de los máximos;
 *  - es sensible a congelación (o a calor) si alguno de sus lotes lo es;
 *  - si el mínimo queda ≥ al máximo, los lotes no pueden compartir el termo.
 */
public final class ContainerLimitsCalculator {
    private ContainerLimitsCalculator() { }

    /** Rango combinado de los lotes (no vacíos). Lanza IllegalArgumentException si no hay rango común. */
    public static TemperatureLimits combine(String contenedor, List<LotLimits> lots) {
        if (lots == null || lots.isEmpty()) throw new IllegalArgumentException("El termo no tiene lotes");
        double min = Double.NEGATIVE_INFINITY;
        double max = Double.POSITIVE_INFINITY;
        boolean freezeSensitive = false;
        boolean heatSensitive = false;
        for (LotLimits lot : lots) {
            min = Math.max(min, lot.minTemp());
            max = Math.min(max, lot.maxTemp());
            freezeSensitive |= lot.freezeSensitive();
            heatSensitive |= lot.heatSensitive();
        }
        if (min >= max) {
            throw new IllegalArgumentException("Los lotes del termo " + contenedor + " no tienen un rango común");
        }
        return new TemperatureLimits("lotes del termo " + contenedor, min, max, freezeSensitive, heatSensitive, lots);
    }

    /**
     * Lotes con los que el candidato no comparte ningún rango. Si los lotes existentes ya son compatibles
     * entre sí, el candidato cabe en el termo si y solo si esta lista está vacía (en una recta, intervalos
     * que se cruzan de a dos tienen una parte común a todos).
     */
    public static List<LotLimits> conflicts(LotLimits candidate, List<LotLimits> existing) {
        return existing.stream()
                .filter(lot -> Math.max(candidate.minTemp(), lot.minTemp()) >= Math.min(candidate.maxTemp(), lot.maxTemp()))
                .toList();
    }

    /** "Varicela (lote VZ9, -50,0 a -15,0 °C)" */
    public static String describe(LotLimits lot) {
        return lot.lot().vaccine() + " (lote " + lot.lot().lotNumber() + ", " + range(lot.minTemp(), lot.maxTemp()) + ")";
    }

    /** "2,0 a 8,0 °C" */
    public static String range(double min, double max) {
        return AlertTexts.number(min) + " a " + AlertTexts.number(max) + " °C";
    }
}
