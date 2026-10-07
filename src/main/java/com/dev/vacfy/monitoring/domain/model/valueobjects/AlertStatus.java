package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.util.EnumSet;
import java.util.Set;

public enum AlertStatus {
    /** Alerta abierta, nadie la ha visto. */
    ACTIVE,
    /** La enfermera la vio, pero la condición sigue. */
    ACKNOWLEDGED,
    /** La condición terminó. */
    RESOLVED;

    public static final Set<AlertStatus> OPEN = EnumSet.of(ACTIVE, ACKNOWLEDGED);

    public boolean isOpen() {
        return OPEN.contains(this);
    }
}
