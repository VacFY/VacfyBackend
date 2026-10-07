package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.PairingCode;

import java.util.Optional;

/** Emparejamiento por QR (fase OLED): códigos temporales que reemplazan a la clave fija durante unos minutos. */
public interface PairingService {
    /**
     * Genera un código temporal para un termo registrado y activo; el nuevo reemplaza al anterior.
     * Vacío si el termo no está registrado o está inactivo.
     */
    Optional<PairingCode> issueTemporaryCode(String contenedor);
}
