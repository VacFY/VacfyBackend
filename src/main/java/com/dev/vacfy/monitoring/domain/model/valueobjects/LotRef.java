package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.time.LocalDate;

/** Datos de un lote que necesitan las reglas para armar mensajes. */
public record LotRef(Long lotId, String contenedor, String vaccine, String lotNumber, LocalDate expiryDate) {
    public AffectedLot toAffected() {
        return new AffectedLot(lotId, vaccine, lotNumber, expiryDate == null ? null : expiryDate.toString());
    }

    /** "Pentavalente (lote AB123)" */
    public String label() {
        return vaccine + " (lote " + lotNumber + ")";
    }
}
