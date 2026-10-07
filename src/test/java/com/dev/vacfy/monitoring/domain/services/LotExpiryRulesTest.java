package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertEvent;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertSeverity;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotRef;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LotExpiryRulesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
    private final LotExpiryRules rules = new LotExpiryRules(30, 7);

    private static LotRef lotExpiringIn(long days) {
        return new LotRef(7L, "001", "Pentavalente", "AB123", TODAY.plusDays(days));
    }

    @Test
    void stagesAtTheBoundaries() {
        assertEquals(LotExpiryRules.Stage.OK, rules.stage(TODAY.plusDays(31), TODAY));
        assertEquals(LotExpiryRules.Stage.EXPIRING, rules.stage(TODAY.plusDays(30), TODAY));
        assertEquals(LotExpiryRules.Stage.EXPIRING, rules.stage(TODAY.plusDays(8), TODAY));
        assertEquals(LotExpiryRules.Stage.EXPIRING_SOON, rules.stage(TODAY.plusDays(7), TODAY));
        assertEquals(LotExpiryRules.Stage.EXPIRING_SOON, rules.stage(TODAY, TODAY), "vence hoy: todavía se puede usar");
        assertEquals(LotExpiryRules.Stage.EXPIRED, rules.stage(TODAY.minusDays(1), TODAY));
    }

    @Test
    void farFromExpiryProducesNothing() {
        assertTrue(rules.evaluate(lotExpiringIn(90), TODAY, null, false).isEmpty());
    }

    @Test
    void thirtyDaysOpensWarning() {
        List<AlertEvent> events = rules.evaluate(lotExpiringIn(30), TODAY, null, false);
        assertEquals(1, events.size());
        AlertEvent event = events.getFirst();
        assertTrue(event.isOpen());
        assertEquals(AlertType.LOT_EXPIRING, event.type());
        assertEquals(AlertSeverity.WARNING, event.severity());
        assertEquals("Lote AB123 (Pentavalente) vence el 06/11/2026", event.title());
        assertEquals("AB123", event.affectedLots().getFirst().lotNumber());
    }

    @Test
    void openWarningIsNotDuplicated() {
        assertTrue(rules.evaluate(lotExpiringIn(20), TODAY, AlertSeverity.WARNING, false).isEmpty());
    }

    @Test
    void sevenDaysEscalatesTheSameAlertToCritical() {
        List<AlertEvent> events = rules.evaluate(lotExpiringIn(7), TODAY, AlertSeverity.WARNING, false);
        assertEquals(1, events.size());
        assertEquals(AlertEvent.Action.ESCALATE, events.getFirst().action());
        assertEquals(AlertSeverity.CRITICAL, events.getFirst().severity());
        assertTrue(rules.evaluate(lotExpiringIn(3), TODAY, AlertSeverity.CRITICAL, false).isEmpty());
    }

    @Test
    void registeringALotThatExpiresSoonOpensCriticalDirectly() {
        List<AlertEvent> events = rules.evaluate(lotExpiringIn(2), TODAY, null, false);
        assertEquals(AlertSeverity.CRITICAL, events.getFirst().severity());
        assertTrue(events.getFirst().isOpen());
    }

    @Test
    void expiredLotResolvesExpiringAndOpensExpired() {
        List<AlertEvent> events = rules.evaluate(lotExpiringIn(-1), TODAY, AlertSeverity.CRITICAL, false);
        assertEquals(2, events.size());
        assertEquals(AlertEvent.Action.RESOLVE, events.get(0).action());
        assertEquals(AlertType.LOT_EXPIRING, events.get(0).type());
        AlertEvent expired = events.get(1);
        assertTrue(expired.isOpen());
        assertEquals(AlertType.LOT_EXPIRED, expired.type());
        assertEquals(AlertSeverity.CRITICAL, expired.severity());
        assertTrue(expired.message().startsWith("No usar: lote AB123 de Pentavalente venció el 06/10/2026"), expired.message());
    }

    @Test
    void expiredAlertIsOpenedOnlyOnce() {
        assertTrue(rules.evaluate(lotExpiringIn(-5), TODAY, null, true).isEmpty());
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LotExpiryRules(5, 7));
    }
}
