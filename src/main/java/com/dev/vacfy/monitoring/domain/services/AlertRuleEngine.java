package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AffectedLot;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertEvent;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertRuleSettings;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertSeverity;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotLimits;
import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Motor de reglas de VacTy. Java puro (sin Spring, JPA ni MQTT) para poder
 * probarlo con tests unitarios y, más adelante, portar las mismas reglas al ESP32.
 *
 * Reglas:
 *  - OUT_OF_RANGE: N lecturas seguidas fuera de [min, max]; se cierra al volver con histéresis.
 *    Si el rango sale de los lotes del termo, el mensaje nombra las vacunas en riesgo y es CRITICAL
 *    cuando el frío daña a alguna sensible a congelación o el calor a alguna sensible al calor.
 *  - RAPID_CHANGE: la temperatura (mediana de las últimas N lecturas) varía >= delta dentro de la ventana.
 *  - SENSOR_OFFLINE: no llegan lecturas durante offlineAfter.
 *  - INVALID_READING: N lecturas seguidas nulas o físicamente imposibles.
 */
public class AlertRuleEngine {
    /** Tope de lotes guardados en affected_lots (la columna admite 4000 caracteres). */
    static final int MAX_AFFECTED_LOTS = 20;

    private final AlertRuleSettings settings;

    public AlertRuleEngine(AlertRuleSettings settings) {
        this.settings = settings;
    }

    public AlertRuleSettings getSettings() {
        return settings;
    }

    public List<AlertEvent> evaluate(ContainerMonitorState state, Double temperature, Double humidity,
                                     Instant now, TemperatureLimits limits) {
        List<AlertEvent> events = new ArrayList<>();
        Instant previous = state.getLastReadingAt();
        if (previous != null && Duration.between(previous, now).compareTo(settings.offlineAfter()) > 0) {
            // Hubo un corte de datos: no comparar con lecturas viejas
            state.window().clear();
            state.recentRaw().clear();
        }
        state.setLastReadingAt(now);

        if (state.isOpen(AlertType.SENSOR_OFFLINE)) {
            events.add(resolve(state, AlertType.SENSOR_OFFLINE, temperature, "El contenedor volvió a enviar datos."));
        }

        if (isInvalid(temperature, humidity)) {
            state.setInvalidCount(state.getInvalidCount() + 1);
            if (!state.isOpen(AlertType.INVALID_READING) && state.getInvalidCount() >= settings.consecutiveReadings()) {
                events.add(open(state, AlertType.INVALID_READING, AlertSeverity.WARNING, temperature,
                        AlertTexts.fallbackTitle(AlertType.INVALID_READING, state.getContenedor()),
                        "El sensor está enviando lecturas inválidas. Revise la conexión del sensor.", List.of()));
            }
            return events;
        }

        state.setInvalidCount(0);
        if (state.isOpen(AlertType.INVALID_READING)) {
            events.add(resolve(state, AlertType.INVALID_READING, temperature, "El sensor volvió a leer correctamente."));
        }

        evaluateRange(state, temperature, limits, events);
        evaluateRapidChange(state, temperature, now, events);
        return events;
    }

    /** Llamar periódicamente: abre SENSOR_OFFLINE si el contenedor dejó de enviar lecturas. */
    public Optional<AlertEvent> checkOffline(ContainerMonitorState state, Instant now) {
        Instant last = state.getLastReadingAt();
        if (last == null || state.isOpen(AlertType.SENSOR_OFFLINE)) return Optional.empty();
        Duration silence = Duration.between(last, now);
        if (silence.compareTo(settings.offlineAfter()) <= 0) return Optional.empty();
        return Optional.of(open(state, AlertType.SENSOR_OFFLINE, AlertSeverity.WARNING, null,
                AlertTexts.fallbackTitle(AlertType.SENSOR_OFFLINE, state.getContenedor()),
                "Sin datos del contenedor desde hace " + silence.toMinutes() + " min. "
                        + "Revise la temperatura manualmente.", List.of()));
    }

    private void evaluateRange(ContainerMonitorState state, double temperature, TemperatureLimits limits,
                               List<AlertEvent> events) {
        if (limits.isOutOfRange(temperature)) {
            state.setOutOfRangeCount(state.getOutOfRangeCount() + 1);
        } else {
            state.setOutOfRangeCount(0);
        }

        if (!state.isOpen(AlertType.OUT_OF_RANGE)) {
            if (state.getOutOfRangeCount() >= settings.consecutiveReadings()) {
                events.add(open(state, outOfRange(state.getContenedor(), temperature, limits)));
            }
            return;
        }

        double h = settings.hysteresis();
        boolean backInside = temperature >= limits.minTemp() + h && temperature <= limits.maxTemp() - h;
        if (backInside) {
            state.setOutOfRangeCount(0);
            events.add(resolve(state, AlertType.OUT_OF_RANGE, temperature,
                    "Temperatura de vuelta en rango: " + AlertTexts.number(temperature) + " °C."));
        }
    }

    private void evaluateRapidChange(ContainerMonitorState state, double temperature, Instant now,
                                     List<AlertEvent> events) {
        // Mediana de las últimas N lecturas: un pico aislado del DHT22 no cuenta como cambio brusco
        var recent = state.recentRaw();
        recent.addLast(temperature);
        while (recent.size() > settings.consecutiveReadings()) {
            recent.pollFirst();
        }
        double smoothed = median(recent);

        var window = state.window();
        Instant cutoff = now.minus(settings.rapidChangeWindow());
        while (!window.isEmpty() && window.peekFirst().at().isBefore(cutoff)) {
            window.pollFirst();
        }
        window.addLast(new ContainerMonitorState.Sample(now, smoothed));

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (var sample : window) {
            min = Math.min(min, sample.temperature());
            max = Math.max(max, sample.temperature());
        }
        double spread = max - min;

        if (!state.isOpen(AlertType.RAPID_CHANGE) && spread >= settings.rapidChangeDelta()) {
            events.add(open(state, AlertType.RAPID_CHANGE, AlertSeverity.WARNING, temperature,
                    AlertTexts.fallbackTitle(AlertType.RAPID_CHANGE, state.getContenedor()), String.format(AlertTexts.ES,
                    "Cambio brusco: la temperatura varió %.1f °C en menos de %d min (ahora %.1f °C). "
                            + "¿Se abrió el termo o se agotaron los paquetes fríos?",
                    spread, settings.rapidChangeWindow().toMinutes(), temperature), List.of()));
        } else if (state.isOpen(AlertType.RAPID_CHANGE) && spread < settings.rapidChangeDelta()) {
            events.add(resolve(state, AlertType.RAPID_CHANGE, temperature, "La temperatura se estabilizó."));
        }
    }

    private static double median(java.util.Collection<Double> values) {
        double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        int mid = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2.0;
    }

    private boolean isInvalid(Double temperature, Double humidity) {
        if (temperature == null || temperature.isNaN()) return true;
        if (temperature < settings.physicalMin() || temperature > settings.physicalMax()) return true;
        return humidity != null && (humidity.isNaN() || humidity < 0 || humidity > 100);
    }

    /**
     * Arma la alerta OUT_OF_RANGE. Con lotes: nombra los que quedaron fuera de su rango y es CRITICAL si
     * el frío afecta a alguno sensible a congelación o el calor a alguno sensible al calor.
     * Sin lotes: usa el perfil del contenedor, como antes.
     */
    static AlertEvent outOfRange(String contenedor, double temperature, TemperatureLimits limits) {
        boolean tooCold = temperature < limits.minTemp();
        String reading = AlertTexts.number(temperature) + " °C";

        if (!limits.basedOnLots()) {
            boolean critical = tooCold ? limits.freezeSensitive() : limits.heatSensitive();
            String message = String.format(AlertTexts.ES, "Temperatura %s: %s (rango %s: %.1f – %.1f °C).%s",
                    tooCold ? "por debajo del mínimo" : "por encima del máximo", reading, limits.profileName(),
                    limits.minTemp(), limits.maxTemp(),
                    critical ? (tooCold ? " Riesgo de congelación" : " Riesgo por calor") + ": no use las vacunas hasta evaluarlas." : "");
            return AlertEvent.open(AlertType.OUT_OF_RANGE, critical ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                    temperature, AlertTexts.outOfRangeTitle(contenedor, temperature, tooCold, critical), message, List.of());
        }

        List<LotLimits> affected = limits.lots().stream()
                .filter(lot -> tooCold ? lot.isTooCold(temperature) : lot.isTooHot(temperature))
                .toList();
        List<LotLimits> atRisk = affected.stream()
                .filter(lot -> tooCold ? lot.freezeSensitive() : lot.heatSensitive())
                .toList();
        boolean critical = !atRisk.isEmpty();

        StringBuilder message = new StringBuilder(reading).append(": ");
        if (critical) {
            message.append(tooCold ? "riesgo de congelación para " : "riesgo por calor para ").append(refs(atRisk)).append('.');
            List<LotLimits> others = affected.stream().filter(lot -> !atRisk.contains(lot)).toList();
            if (!others.isEmpty()) message.append(" También fuera de rango: ").append(refs(others)).append('.');
        } else {
            message.append(tooCold ? "por debajo del mínimo para " : "por encima del máximo para ").append(refs(affected)).append('.');
        }
        message.append(" Rango del termo: ").append(AlertTexts.number(limits.minTemp())).append(" – ")
                .append(AlertTexts.number(limits.maxTemp())).append(" °C.");
        if (critical) message.append(" No use las vacunas hasta evaluarlas.");

        List<AffectedLot> affectedLots = affected.stream().limit(MAX_AFFECTED_LOTS).map(lot -> lot.lot().toAffected()).toList();
        return AlertEvent.open(AlertType.OUT_OF_RANGE, critical ? AlertSeverity.CRITICAL : AlertSeverity.WARNING, temperature,
                AlertTexts.outOfRangeTitle(contenedor, temperature, tooCold, critical), message.toString(), affectedLots);
    }

    private static String refs(List<LotLimits> lots) {
        return AlertTexts.lotList(lots.stream().map(LotLimits::lot).toList());
    }

    private AlertEvent open(ContainerMonitorState state, AlertEvent event) {
        state.markOpen(event.type());
        return event;
    }

    private AlertEvent open(ContainerMonitorState state, AlertType type, AlertSeverity severity, Double value,
                            String title, String message, List<AffectedLot> affectedLots) {
        state.markOpen(type);
        return AlertEvent.open(type, severity, value, title, message, affectedLots);
    }

    private AlertEvent resolve(ContainerMonitorState state, AlertType type, Double value, String message) {
        state.markResolved(type);
        return AlertEvent.resolve(type, value, message);
    }
}
