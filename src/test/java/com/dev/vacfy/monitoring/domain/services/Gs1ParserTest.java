package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.exceptions.Gs1ParseException;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Gs1Data;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class Gs1ParserTest {
    private static final String GS = String.valueOf(Gs1Parser.GS);
    /** GTIN de ejemplo con dígito verificador válido. */
    private static final String GTIN = "08901234567890";

    @Test
    void rawScanWithGroupSeparator() {
        Gs1Data data = Gs1Parser.parse("01" + GTIN + "17271231" + "10AB1234" + GS + "21SN0001");
        assertEquals(GTIN, data.gtin());
        assertEquals(LocalDate.of(2027, 12, 31), data.expiry());
        assertEquals("AB1234", data.lot());
        assertEquals("SN0001", data.serial());
    }

    @Test
    void rawScanWithSymbologyIdentifier() {
        Gs1Data data = Gs1Parser.parse("]d2" + "01" + GTIN + "17271231" + GS + "10AB1234");
        assertEquals(GTIN, data.gtin());
        assertEquals(LocalDate.of(2027, 12, 31), data.expiry());
        assertEquals("AB1234", data.lot());
        assertNull(data.serial());
    }

    @Test
    void humanReadableWithParentheses() {
        Gs1Data data = Gs1Parser.parse("(01)" + GTIN + "(17)271231(10)AB1234");
        assertEquals(new Gs1Data(GTIN, LocalDate.of(2027, 12, 31), "AB1234", null), data);
    }

    @Test
    void humanReadableToleratesSpaces() {
        Gs1Data data = Gs1Parser.parse(" (01) " + GTIN + " (17) 271231 (10) AB1234 ");
        assertEquals("AB1234", data.lot());
    }

    @Test
    void onlyGtin() {
        assertEquals(new Gs1Data(GTIN, null, null, null), Gs1Parser.parse(GTIN));
    }

    @Test
    void ean13IsPaddedToGtin14() {
        assertEquals(GTIN, Gs1Parser.parse("8901234567890").gtin());
    }

    @Test
    void dayZeroMeansLastDayOfMonth() {
        assertEquals(LocalDate.of(2027, 2, 28), Gs1Parser.parse("(01)" + GTIN + "(17)270200").expiry());
        assertEquals(LocalDate.of(2028, 2, 29), Gs1Parser.parse("(17)280200(10)L1").expiry());
    }

    @Test
    void invalidCheckDigitIsRejected() {
        var error = assertThrows(Gs1ParseException.class,
                () -> Gs1Parser.parse("(01)08901234567892(17)271231(10)AB1234"));
        assertTrue(error.getMessage().contains("dígito verificador"));
        assertThrows(Gs1ParseException.class, () -> Gs1Parser.parse("08901234567892"));
    }

    @Test
    void lotAtTheEndWithoutGroupSeparator() {
        Gs1Data data = Gs1Parser.parse("01" + GTIN + "17271231" + "10AB1234");
        assertEquals("AB1234", data.lot());
        assertEquals(LocalDate.of(2027, 12, 31), data.expiry());
    }

    @Test
    void otherAisAreIgnored() {
        // 11 = fecha de producción (fija), 30 = cantidad (variable, termina en GS)
        Gs1Data raw = Gs1Parser.parse("01" + GTIN + "11260101" + "3050" + GS + "10AB1234");
        assertEquals("AB1234", raw.lot());
        Gs1Data typed = Gs1Parser.parse("(01)" + GTIN + "(11)260101(30)50(10)AB1234(240)X1");
        assertEquals("AB1234", typed.lot());
    }

    @Test
    void groupSeparatorPlaceholderIsAccepted() {
        assertEquals("SN1", Gs1Parser.parse("01" + GTIN + "10AB1234<GS>21SN1").serial());
    }

    @Test
    void invalidExpiryDateIsRejected() {
        assertThrows(Gs1ParseException.class, () -> Gs1Parser.parse("(17)271331"));
    }

    @Test
    void lotLongerThan20CharactersSuggestsMissingSeparator() {
        var error = assertThrows(Gs1ParseException.class,
                () -> Gs1Parser.parse("01" + GTIN + "10ABCDEFGHIJKLMNOPQRSTUVW"));
        assertTrue(error.getMessage().contains("separador GS"));
    }

    @Test
    void textThatIsNotGs1IsRejected() {
        assertThrows(Gs1ParseException.class, () -> Gs1Parser.parse("AB1234"));
        assertThrows(Gs1ParseException.class, () -> Gs1Parser.parse("   "));
    }
}
