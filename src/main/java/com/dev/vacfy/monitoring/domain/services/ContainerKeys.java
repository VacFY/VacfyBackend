package com.dev.vacfy.monitoring.domain.services;

import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * Claves de vinculación de los termos. Java puro.
 *  - Clave fija: 6 caracteres en formato "XXX-XXX" (se imprime en la etiqueta del termo).
 *  - Código temporal: 6 caracteres sin guion (para el QR de la pantalla OLED).
 * El alfabeto no tiene caracteres que se confunden al leerlos: 0/O, 1/I/L.
 */
public final class ContainerKeys {
    public static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final Pattern CODIGO = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private ContainerKeys() { }

    /** "K7P-29Q" */
    public static String newKey(RandomGenerator random) {
        String chars = randomChars(random, 6);
        return chars.substring(0, 3) + "-" + chars.substring(3);
    }

    /** "K7P29Q" */
    public static String newTemporaryCode(RandomGenerator random) {
        return randomChars(random, 6);
    }

    /** Mayúsculas, sin espacios ni guiones: "k7p 29-q" → "K7P29Q". null → "". */
    public static String normalize(String key) {
        if (key == null) return "";
        return key.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** Códigos de termo válidos: los que puede enviar el ESP32 y caben en un tópico MQTT ("001", "posta-2"). */
    public static boolean isValidCodigo(String codigo) {
        return codigo != null && CODIGO.matcher(codigo).matches();
    }

    private static String randomChars(RandomGenerator random, int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) value.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return value.toString();
    }
}
