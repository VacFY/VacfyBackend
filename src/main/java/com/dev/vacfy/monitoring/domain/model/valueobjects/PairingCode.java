package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.time.Instant;

/** Código temporal de vinculación que el ESP32 muestra como QR. */
public record PairingCode(String contenedor, String codigo, Instant venceEn) {
    /** Contenido del QR: "vacty:<codigo del termo>:<código temporal>". */
    public String qr() {
        return "vacty:" + contenedor + ":" + codigo;
    }
}
