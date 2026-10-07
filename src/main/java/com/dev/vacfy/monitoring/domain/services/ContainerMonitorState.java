package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;

/**
 * Memoria del motor para UN contenedor: qué alertas están abiertas y las lecturas recientes.
 * No es thread-safe: el llamador sincroniza por contenedor.
 */
public class ContainerMonitorState {
    public record Sample(Instant at, double temperature) { }

    private final String contenedor;
    private final EnumSet<AlertType> openAlerts = EnumSet.noneOf(AlertType.class);
    private final Deque<Sample> window = new ArrayDeque<>();
    private final Deque<Double> recentRaw = new ArrayDeque<>();
    private int outOfRangeCount;
    private int invalidCount;
    private Instant lastReadingAt;

    public ContainerMonitorState(String contenedor) {
        this.contenedor = contenedor;
    }

    public String getContenedor() { return contenedor; }

    public boolean isOpen(AlertType type) { return openAlerts.contains(type); }

    public boolean hasOpenAlerts() { return !openAlerts.isEmpty(); }

    public Set<AlertType> getOpenAlerts() { return EnumSet.copyOf(openAlerts); }

    /** Sincroniza con las alertas abiertas guardadas en la base de datos (al reiniciar el backend). */
    public void restoreOpenAlerts(Collection<AlertType> types) {
        openAlerts.clear();
        openAlerts.addAll(types);
    }

    void markOpen(AlertType type) { openAlerts.add(type); }

    void markResolved(AlertType type) { openAlerts.remove(type); }

    Deque<Sample> window() { return window; }

    Deque<Double> recentRaw() { return recentRaw; }

    int getOutOfRangeCount() { return outOfRangeCount; }

    void setOutOfRangeCount(int value) { outOfRangeCount = value; }

    int getInvalidCount() { return invalidCount; }

    void setInvalidCount(int value) { invalidCount = value; }

    public Instant getLastReadingAt() { return lastReadingAt; }

    void setLastReadingAt(Instant at) { lastReadingAt = at; }

    /** Marca el momento desde el que se empieza a vigilar (para detectar "sin datos" tras un reinicio). */
    public void seedLastReadingAt(Instant at) {
        if (lastReadingAt == null) lastReadingAt = at;
    }
}
