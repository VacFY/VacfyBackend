package com.dev.vacfy.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Última telemetría de cada termo, en memoria. Las lecturas en la base de datos están muestreadas
 * (1 cada 30 s); aquí está la más reciente. Tras un reinicio se vacía y el dashboard usa la base de datos.
 */
@Component
public class LiveTelemetryStore {
    public record LiveReading(Double temperatura, Double humedad, Instant receivedAt) { }

    private final Map<String, LiveReading> lastValid = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();

    /** @param valid false si la lectura era nula o imposible (cuenta como "llegaron datos", no como temperatura) */
    public void record(String contenedor, Double temperatura, Double humedad, boolean valid, Instant at) {
        lastSeen.put(contenedor, at);
        if (valid) lastValid.put(contenedor, new LiveReading(temperatura, humedad, at));
    }

    public Optional<LiveReading> lastValid(String contenedor) {
        return Optional.ofNullable(lastValid.get(contenedor));
    }

    public Optional<Instant> lastSeen(String contenedor) {
        return Optional.ofNullable(lastSeen.get(contenedor));
    }

    public Set<String> containers() {
        return Set.copyOf(lastSeen.keySet());
    }
}
