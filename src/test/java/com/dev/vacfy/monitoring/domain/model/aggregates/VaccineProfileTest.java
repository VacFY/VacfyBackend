package com.dev.vacfy.monitoring.domain.model.aggregates;

import com.dev.vacfy.monitoring.domain.model.valueobjects.CareProfile;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VaccineProfileTest {
    private static VaccineProfile pentavalente() {
        return new VaccineProfile("Pentavalente", "Difteria, tos ferina…", 2.0, 8.0, true, false, null, null);
    }

    @Test
    void careInstructionsKeepTheirOrderAndDropBlankLines() {
        VaccineProfile vaccine = pentavalente();
        vaccine.updateCare(CareProfile.ANTI_FREEZE, Arrays.asList(" Se daña al congelarse. ", "", null, "Usar paquetes fríos acondicionados."));
        assertEquals(CareProfile.ANTI_FREEZE, vaccine.getCareProfile());
        assertEquals(List.of("Se daña al congelarse.", "Usar paquetes fríos acondicionados."), vaccine.getCareInstructionList());
    }

    @Test
    void noCareInstructionsIsAnEmptyList() {
        VaccineProfile vaccine = pentavalente();
        assertTrue(vaccine.getCareInstructionList().isEmpty());
        vaccine.updateCare(null, List.of());
        assertNull(vaccine.getCareProfile());
        assertNull(vaccine.getCareInstructions());
    }

    @Test
    void careInstructionsAreLimited() {
        VaccineProfile vaccine = pentavalente();
        assertThrows(IllegalArgumentException.class,
                () -> vaccine.updateCare(CareProfile.ANTI_FREEZE, List.of("x".repeat(1001))));
        assertThrows(IllegalArgumentException.class,
                () -> vaccine.updateCare(CareProfile.ANTI_FREEZE, List.of("una\ndos")));
    }

    @Test
    void careProfilesHaveLabelsWithoutEmojis() {
        assertEquals("Anticongelamiento", CareProfile.ANTI_FREEZE.label());
        assertEquals("Proteger de luz y calor", CareProfile.PROTECT_FROM_LIGHT_AND_HEAT.label());
        assertEquals("Verificar fabricante", CareProfile.CHECK_MANUFACTURER.label());
    }
}
