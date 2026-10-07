package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.LotLimits;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotRef;
import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContainerLimitsCalculatorTest {
    static LotLimits lot(long id, String vaccine, String lot, double min, double max, boolean freeze, boolean heat) {
        return new LotLimits(new LotRef(id, "001", vaccine, lot, LocalDate.of(2027, 12, 31)), min, max, freeze, heat);
    }

    private static final LotLimits PENTAVALENTE = lot(1, "Pentavalente", "AB123", 2.0, 8.0, true, false);
    private static final LotLimits SPR = lot(2, "SPR", "S1", 2.0, 8.0, false, true);
    private static final LotLimits NARROW = lot(3, "Especial", "E1", 4.0, 6.0, false, false);
    private static final LotLimits FROZEN = lot(4, "Varicela congelada", "VZ9", -50.0, -15.0, false, true);

    @Test
    void sameRangeKeepsIt() {
        TemperatureLimits limits = ContainerLimitsCalculator.combine("001", List.of(PENTAVALENTE, SPR));
        assertEquals(2.0, limits.minTemp());
        assertEquals(8.0, limits.maxTemp());
    }

    @Test
    void rangeIsTheHighestMinAndTheLowestMax() {
        TemperatureLimits limits = ContainerLimitsCalculator.combine("001", List.of(PENTAVALENTE, NARROW));
        assertEquals(4.0, limits.minTemp());
        assertEquals(6.0, limits.maxTemp());
    }

    @Test
    void sensitivityComesFromAnyLot() {
        TemperatureLimits limits = ContainerLimitsCalculator.combine("001", List.of(PENTAVALENTE, SPR));
        assertTrue(limits.freezeSensitive());
        assertTrue(limits.heatSensitive());
        TemperatureLimits onlyNarrow = ContainerLimitsCalculator.combine("001", List.of(NARROW));
        assertFalse(onlyNarrow.freezeSensitive());
        assertFalse(onlyNarrow.heatSensitive());
    }

    @Test
    void combinedLimitsKeepTheLots() {
        TemperatureLimits limits = ContainerLimitsCalculator.combine("001", List.of(PENTAVALENTE, SPR));
        assertTrue(limits.basedOnLots());
        assertEquals(2, limits.lots().size());
    }

    @Test
    void noCommonRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ContainerLimitsCalculator.combine("001", List.of(PENTAVALENTE, FROZEN)));
        assertThrows(IllegalArgumentException.class, () -> ContainerLimitsCalculator.combine("001", List.of()));
    }

    @Test
    void touchingRangesAreIncompatible() {
        LotLimits upTo4 = lot(5, "A", "A1", 0.0, 4.0, false, false);
        LotLimits from4 = lot(6, "B", "B1", 4.0, 8.0, false, false);
        assertEquals(List.of(upTo4), ContainerLimitsCalculator.conflicts(from4, List.of(upTo4)));
    }

    @Test
    void conflictsListOnlyTheIncompatibleLots() {
        assertEquals(List.of(PENTAVALENTE, SPR), ContainerLimitsCalculator.conflicts(FROZEN, List.of(PENTAVALENTE, SPR)));
        assertTrue(ContainerLimitsCalculator.conflicts(NARROW, List.of(PENTAVALENTE, SPR)).isEmpty());
    }

    @Test
    void describesLotWithItsRange() {
        assertEquals("Varicela congelada (lote VZ9, -50,0 a -15,0 °C)", ContainerLimitsCalculator.describe(FROZEN));
    }
}
