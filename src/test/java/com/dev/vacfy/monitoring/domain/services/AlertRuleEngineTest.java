package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertEvent;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertRuleSettings;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertSeverity;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertRuleEngineTest {
    private static final TemperatureLimits PAI = new TemperatureLimits("PAI 2–8", 2.0, 8.0, false);
    private static final TemperatureLimits FREEZE_SENSITIVE = new TemperatureLimits("Sensible a congelación", 2.0, 8.0, true);

    private final AlertRuleEngine engine = new AlertRuleEngine(AlertRuleSettings.defaults());
    private final ContainerMonitorState state = new ContainerMonitorState("001");
    private Instant now = Instant.parse("2026-10-07T12:00:00Z");

    /** Envía una lectura y avanza el reloj 2 s (igual que el ESP32). */
    private List<AlertEvent> read(Double temperature, TemperatureLimits limits) {
        var events = engine.evaluate(state, temperature, 50.0, now, limits);
        now = now.plusSeconds(2);
        return events;
    }

    private List<AlertEvent> readAll(TemperatureLimits limits, Double... temperatures) {
        List<AlertEvent> all = new ArrayList<>();
        for (Double t : temperatures) all.addAll(read(t, limits));
        return all;
    }

    private static boolean opened(List<AlertEvent> events, AlertType type) {
        return events.stream().anyMatch(e -> e.isOpen() && e.type() == type);
    }

    private static boolean resolved(List<AlertEvent> events, AlertType type) {
        return events.stream().anyMatch(e -> !e.isOpen() && e.type() == type);
    }

    @Test
    void normalTemperatureProducesNoAlerts() {
        assertTrue(readAll(PAI, 5.0, 5.1, 4.9, 5.0).isEmpty());
        assertFalse(state.hasOpenAlerts());
    }

    @Test
    void threeReadingsAboveMaxOpenOutOfRange() {
        assertTrue(readAll(PAI, 9.0, 9.0).isEmpty(), "dos lecturas no bastan");
        var events = read(9.0, PAI);
        assertTrue(opened(events, AlertType.OUT_OF_RANGE));
        assertEquals(AlertSeverity.WARNING, events.getFirst().severity());
        assertTrue(state.isOpen(AlertType.OUT_OF_RANGE));
    }

    @Test
    void singleSpikeDoesNotOpenAlert() {
        var events = readAll(PAI, 5.0, 12.0, 5.0, 5.0);
        assertFalse(opened(events, AlertType.OUT_OF_RANGE));
    }

    @Test
    void outOfRangeClosesOnlyWithHysteresis() {
        readAll(PAI, 9.0, 9.0, 9.0);
        assertFalse(resolved(read(7.8, PAI), AlertType.OUT_OF_RANGE), "7.8 está dentro de la banda de histéresis");
        assertTrue(resolved(read(7.4, PAI), AlertType.OUT_OF_RANGE));
        assertFalse(state.isOpen(AlertType.OUT_OF_RANGE));
    }

    @Test
    void alertIsOpenedOnlyOncePerEpisode() {
        var events = readAll(PAI, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0);
        assertEquals(1, events.stream().filter(e -> e.type() == AlertType.OUT_OF_RANGE).count());
    }

    @Test
    void freezeSensitiveBelowMinIsCritical() {
        var events = readAll(FREEZE_SENSITIVE, 1.0, 1.0, 1.0);
        var alert = events.stream().filter(e -> e.type() == AlertType.OUT_OF_RANGE).findFirst().orElseThrow();
        assertEquals(AlertSeverity.CRITICAL, alert.severity());
    }

    @Test
    void notFreezeSensitiveBelowMinIsWarning() {
        var events = readAll(PAI, 1.0, 1.0, 1.0);
        var alert = events.stream().filter(e -> e.type() == AlertType.OUT_OF_RANGE).findFirst().orElseThrow();
        assertEquals(AlertSeverity.WARNING, alert.severity());
    }

    /** N lecturas iguales seguidas (el ESP32 publica cada 2 s). */
    private List<AlertEvent> hold(double temperature, int times) {
        List<AlertEvent> all = new ArrayList<>();
        for (int i = 0; i < times; i++) all.addAll(read(temperature, PAI));
        return all;
    }

    @Test
    void rapidChangeWithinWindowOpensAlert() {
        hold(4.0, 3);
        now = now.plus(Duration.ofSeconds(90));
        assertTrue(opened(hold(7.0, 3), AlertType.RAPID_CHANGE));
    }

    @Test
    void singleSpikeDoesNotOpenRapidChange() {
        var events = readAll(PAI, 5.0, 5.0, 5.0, 12.0, 5.0, 5.0);
        assertFalse(opened(events, AlertType.RAPID_CHANGE));
    }

    @Test
    void changeAfterDataGapDoesNotOpenRapidChange() {
        hold(4.0, 3);
        now = now.plus(Duration.ofMinutes(6));
        assertFalse(opened(hold(7.0, 3), AlertType.RAPID_CHANGE));
    }

    @Test
    void slowDriftDoesNotOpenRapidChange() {
        // +1.5 °C en 10 min, una lectura cada 10 s
        List<AlertEvent> all = new ArrayList<>();
        for (int i = 0; i <= 60; i++) {
            all.addAll(engine.evaluate(state, 4.0 + 1.5 * i / 60.0, 50.0, now, PAI));
            now = now.plusSeconds(10);
        }
        assertFalse(opened(all, AlertType.RAPID_CHANGE));
    }

    @Test
    void rapidChangeResolvesWhenStable() {
        hold(4.0, 3);
        now = now.plus(Duration.ofMinutes(1));
        assertTrue(opened(hold(7.0, 3), AlertType.RAPID_CHANGE));
        now = now.plus(Duration.ofMinutes(6));
        assertTrue(resolved(hold(7.0, 1), AlertType.RAPID_CHANGE));
    }

    @Test
    void sensorOfflineAfterSilenceAndResolvesOnNextReading() {
        read(5.0, PAI);
        assertTrue(engine.checkOffline(state, now.plus(Duration.ofSeconds(60))).isEmpty());
        var offline = engine.checkOffline(state, now.plus(Duration.ofMinutes(3)));
        assertTrue(offline.isPresent());
        assertEquals(AlertType.SENSOR_OFFLINE, offline.get().type());
        assertTrue(engine.checkOffline(state, now.plus(Duration.ofMinutes(4))).isEmpty(), "no se repite");

        now = now.plus(Duration.ofMinutes(5));
        assertTrue(resolved(read(5.0, PAI), AlertType.SENSOR_OFFLINE));
    }

    @Test
    void invalidReadingsOpenAlertAndAreIgnoredForRange() {
        var events = readAll(PAI, null, Double.NaN, 150.0);
        assertTrue(opened(events, AlertType.INVALID_READING));
        assertFalse(opened(events, AlertType.OUT_OF_RANGE));
        assertTrue(resolved(read(5.0, PAI), AlertType.INVALID_READING));
    }

    @Test
    void restoredOpenAlertIsNotOpenedAgain() {
        state.restoreOpenAlerts(List.of(AlertType.OUT_OF_RANGE));
        var events = readAll(PAI, 9.0, 9.0, 9.0);
        assertFalse(opened(events, AlertType.OUT_OF_RANGE));
        assertTrue(resolved(read(5.0, PAI), AlertType.OUT_OF_RANGE));
    }
}
