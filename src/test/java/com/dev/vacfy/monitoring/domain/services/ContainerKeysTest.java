package com.dev.vacfy.monitoring.domain.services;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ContainerKeysTest {
    private final Random random = new Random(42);

    @Test
    void keyHasFormatXxxDashXxxWithoutConfusingCharacters() {
        for (int i = 0; i < 500; i++) {
            String key = ContainerKeys.newKey(random);
            assertTrue(key.matches("[A-Z2-9]{3}-[A-Z2-9]{3}"), key);
            assertFalse(key.matches(".*[01OIL].*"), key);
        }
    }

    @Test
    void temporaryCodeHasSixCharactersWithoutDash() {
        String code = ContainerKeys.newTemporaryCode(random);
        assertEquals(6, code.length());
        assertTrue(code.chars().allMatch(c -> ContainerKeys.ALPHABET.indexOf(c) >= 0), code);
    }

    @Test
    void normalizeIgnoresCaseSpacesAndDash() {
        assertEquals("K7P29Q", ContainerKeys.normalize("k7p-29q"));
        assertEquals("K7P29Q", ContainerKeys.normalize(" K7P 29Q "));
        assertEquals("", ContainerKeys.normalize(null));
    }

    @Test
    void codigoMustFitInAnMqttTopic() {
        assertTrue(ContainerKeys.isValidCodigo("001"));
        assertTrue(ContainerKeys.isValidCodigo("posta_2-b"));
        assertFalse(ContainerKeys.isValidCodigo("vacty/001"));
        assertFalse(ContainerKeys.isValidCodigo("00#"));
        assertFalse(ContainerKeys.isValidCodigo(""));
        assertFalse(ContainerKeys.isValidCodigo("x".repeat(33)));
    }
}
