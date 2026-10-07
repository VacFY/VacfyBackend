package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.exceptions.Gs1ParseException;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Gs1Data;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee el código GS1 de una caja de vacunas. Java puro, sin estado.
 *
 * Acepta:
 *  - el texto crudo del escáner, con o sin identificador de simbología ("]d2", "]C1", "]Q3"…)
 *    y con el separador GS (ASCII 29) después de los campos de longitud variable;
 *  - el formato legible que se escribe a mano: "(01)08901234567890(17)271231(10)AB1234";
 *  - solo el GTIN (8, 12, 13 o 14 dígitos; se completa a 14 con ceros).
 *
 * Usa los AIs 01 (GTIN), 17 (vencimiento), 10 (lote) y 21 (serie). Los demás se saltan sin fallar.
 */
public final class Gs1Parser {
    public static final char GS = '\u001d';
    private static final int MAX_LOT_LENGTH = 20;
    private static final int MAX_SERIAL_LENGTH = 20;

    private static final Pattern HUMAN_READABLE = Pattern.compile("\\((\\d{2,4})\\)([^()]*)");
    private static final Pattern GTIN_ONLY = Pattern.compile("\\d{8}|\\d{12,14}");
    /** Algunos escáneres o teclados no pueden enviar GS y lo escriben así. */
    private static final Pattern GS_PLACEHOLDER = Pattern.compile("(?i)<GS>|\\{GS}|\\[GS]");

    /**
     * AIs de longitud predefinida (tabla de la especificación general GS1): por los dos primeros dígitos,
     * longitud total del campo contando el AI. Todos los demás son variables y terminan en GS o al final.
     */
    private static final Map<String, Integer> PREDEFINED_LENGTH = Map.ofEntries(
            Map.entry("00", 20), Map.entry("01", 16), Map.entry("02", 16), Map.entry("03", 16), Map.entry("04", 18),
            Map.entry("11", 8), Map.entry("12", 8), Map.entry("13", 8), Map.entry("14", 8), Map.entry("15", 8),
            Map.entry("16", 8), Map.entry("17", 8), Map.entry("18", 8), Map.entry("19", 8), Map.entry("20", 4),
            Map.entry("31", 10), Map.entry("32", 10), Map.entry("33", 10), Map.entry("34", 10), Map.entry("35", 10),
            Map.entry("36", 10), Map.entry("41", 16));

    private Gs1Parser() { }

    public static Gs1Data parse(String input) {
        if (input == null || input.isBlank()) throw new Gs1ParseException("Escanee o escriba un código.");
        String code = normalize(input);
        if (code.isEmpty()) throw new Gs1ParseException("Escanee o escriba un código.");

        if (GTIN_ONLY.matcher(code).matches()) {
            return new Gs1Data(validGtin(padGtin(code)), null, null, null);
        }
        Map<String, String> fields = code.indexOf('(') >= 0 ? parseHumanReadable(code) : parseElementString(code);
        return build(fields);
    }

    /** Valida el dígito verificador de un GTIN de 14 dígitos (módulo 10 de GS1). */
    public static boolean isValidGtin(String gtin) {
        if (gtin == null || !gtin.matches("\\d{14}")) return false;
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            int digit = gtin.charAt(i) - '0';
            sum += i % 2 == 0 ? digit * 3 : digit;
        }
        int check = (10 - sum % 10) % 10;
        return check == gtin.charAt(13) - '0';
    }

    // ---------------------------------------------------------------------------------------------

    private static String normalize(String input) {
        String code = GS_PLACEHOLDER.matcher(input).replaceAll(String.valueOf(GS));
        code = code.replaceAll("[ \\t\\r\\n]", "");
        // Identificador de simbología del escáner: "]d2" (DataMatrix), "]C1" (GS1-128), "]Q3" (QR)…
        if (code.length() >= 3 && code.charAt(0) == ']') code = code.substring(3);
        return code;
    }

    private static Map<String, String> parseHumanReadable(String code) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = HUMAN_READABLE.matcher(code);
        int expectedStart = 0;
        while (matcher.find()) {
            if (matcher.start() != expectedStart) throw unreadable();
            fields.putIfAbsent(matcher.group(1), matcher.group(2).replace(String.valueOf(GS), ""));
            expectedStart = matcher.end();
        }
        if (fields.isEmpty() || expectedStart != code.length()) throw unreadable();
        return fields;
    }

    private static Map<String, String> parseElementString(String code) {
        Map<String, String> fields = new LinkedHashMap<>();
        int pos = 0;
        while (pos < code.length()) {
            if (code.charAt(pos) == GS) {
                pos++;
                continue;
            }
            int aiLength = aiLength(code, pos);
            String ai = code.substring(pos, pos + aiLength);
            pos += aiLength;

            Integer total = PREDEFINED_LENGTH.get(ai.substring(0, 2));
            String value;
            if (total != null) {
                int end = pos + total - aiLength;
                if (end > code.length()) {
                    throw new Gs1ParseException("El código está incompleto: el campo (" + ai + ") está cortado.");
                }
                value = code.substring(pos, end);
                pos = end;
            } else {
                int end = code.indexOf(GS, pos);
                if (end < 0) end = code.length();
                value = code.substring(pos, end);
                pos = end;
            }
            fields.putIfAbsent(ai, value);
        }
        return fields;
    }

    /** Longitud del AI según sus dos primeros dígitos (2, 3 o 4 dígitos). */
    private static int aiLength(String code, int pos) {
        if (pos + 2 > code.length() || !isDigits(code, pos, pos + 2)) throw unreadable();
        int length = switch (code.substring(pos, pos + 2)) {
            case "23", "24", "25", "40", "41", "42", "71" -> 3;
            case "31", "32", "33", "34", "35", "36", "39", "43", "70", "72", "80", "81", "82" -> 4;
            default -> 2;
        };
        if (pos + length > code.length() || !isDigits(code, pos, pos + length)) throw unreadable();
        return length;
    }

    private static Gs1Data build(Map<String, String> fields) {
        String gtin = fields.containsKey("01") ? validGtin(fields.get("01")) : null;
        LocalDate expiry = fields.containsKey("17") ? parseExpiry(fields.get("17")) : null;
        String lot = limited(fields.get("10"), "10", "lote", MAX_LOT_LENGTH);
        String serial = limited(fields.get("21"), "21", "serie", MAX_SERIAL_LENGTH);
        if (gtin == null && expiry == null && lot == null && serial == null) {
            throw new Gs1ParseException("El código no trae GTIN (01), vencimiento (17), lote (10) ni serie (21).");
        }
        return new Gs1Data(gtin, expiry, lot, serial);
    }

    private static String validGtin(String value) {
        if (value == null || !value.matches("\\d{14}")) {
            throw new Gs1ParseException("GTIN inválido: debe tener 14 dígitos.");
        }
        if (!isValidGtin(value)) {
            throw new Gs1ParseException("GTIN inválido: " + value + " no pasa el dígito verificador. Revise el número.");
        }
        return value;
    }

    private static String padGtin(String digits) {
        return "0".repeat(14 - digits.length()) + digits;
    }

    /** AAMMDD. Día 00 = último día del mes (regla GS1). */
    private static LocalDate parseExpiry(String value) {
        if (value == null || !value.matches("\\d{6}")) {
            throw new Gs1ParseException("Fecha de vencimiento inválida (17): use AAMMDD, p. ej. 271231.");
        }
        int year = 2000 + Integer.parseInt(value.substring(0, 2));
        int month = Integer.parseInt(value.substring(2, 4));
        int day = Integer.parseInt(value.substring(4, 6));
        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            return day == 0 ? yearMonth.atEndOfMonth() : yearMonth.atDay(day);
        } catch (DateTimeException e) {
            throw new Gs1ParseException("Fecha de vencimiento inválida (17): " + value + " no es una fecha real.");
        }
    }

    private static String limited(String value, String ai, String label, int maxLength) {
        if (value == null) return null;
        if (value.isEmpty()) throw new Gs1ParseException("El campo " + label + " (" + ai + ") está vacío.");
        if (value.length() > maxLength) {
            throw new Gs1ParseException("El " + label + " (" + ai + ") tiene más de " + maxLength
                    + " caracteres: probablemente falta el separador GS después de él.");
        }
        return value;
    }

    private static boolean isDigits(String code, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = code.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static Gs1ParseException unreadable() {
        return new Gs1ParseException("No se reconoce el código. Escanéelo de nuevo, escríbalo como "
                + "(01)GTIN(17)AAMMDD(10)LOTE o llene los datos a mano.");
    }
}
