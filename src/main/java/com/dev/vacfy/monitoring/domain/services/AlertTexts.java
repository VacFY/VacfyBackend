package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotRef;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Títulos y piezas de texto de las alertas. El título es corto (para la notificación del celular);
 * el mensaje, que arma cada regla, es para la enfermera.
 */
public final class AlertTexts {
    /** Español con coma decimal ("1,2 °C"). */
    public static final Locale ES = Locale.forLanguageTag("es");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_NAMED_LOTS = 5;

    private AlertTexts() { }

    /** 1.25 → "1,3" */
    public static String number(double value) {
        return String.format(ES, "%.1f", value);
    }

    public static String date(java.time.LocalDate date) {
        return DATE.format(date);
    }

    /** "Pentavalente (lote AB123) y Hepatitis B (lote HB77)"; con más de 5 lotes, "… y 3 lotes más". */
    public static String lotList(List<LotRef> lots) {
        List<String> labels = lots.stream().limit(MAX_NAMED_LOTS).map(LotRef::label).toList();
        int rest = lots.size() - labels.size();
        if (rest > 0) return String.join(", ", labels) + " y " + rest + (rest == 1 ? " lote más" : " lotes más");
        if (labels.size() == 1) return labels.getFirst();
        return String.join(", ", labels.subList(0, labels.size() - 1)) + " y " + labels.getLast();
    }

    public static String outOfRangeTitle(String contenedor, double temperature, boolean tooCold, boolean critical) {
        if (critical) return (tooCold ? "Riesgo de congelación" : "Riesgo por calor") + " en termo " + contenedor;
        return (tooCold ? "Temperatura baja" : "Temperatura alta") + " en termo " + contenedor + ": " + number(temperature) + " °C";
    }

    public static String lotExpiringTitle(LotRef lot) {
        return "Lote " + lot.lotNumber() + " (" + lot.vaccine() + ") vence el " + date(lot.expiryDate());
    }

    public static String lotExpiredTitle(LotRef lot) {
        return "Lote " + lot.lotNumber() + " (" + lot.vaccine() + ") vencido";
    }

    /** Título para alertas guardadas antes de que existiera el campo, o sin datos para uno mejor. */
    public static String fallbackTitle(AlertType type, String contenedor) {
        return switch (type) {
            case OUT_OF_RANGE -> "Temperatura fuera de rango en termo " + contenedor;
            case RAPID_CHANGE -> "Cambio brusco de temperatura en termo " + contenedor;
            case SENSOR_OFFLINE -> "Termo " + contenedor + " sin datos";
            case INVALID_READING -> "Sensor con fallas en termo " + contenedor;
        };
    }
}
