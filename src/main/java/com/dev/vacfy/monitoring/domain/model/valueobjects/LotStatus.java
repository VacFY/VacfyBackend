package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.util.EnumSet;
import java.util.Set;

public enum LotStatus {
    /** En el termo y disponible. */
    ACTIVE,
    /** Se terminó de usar. */
    USED,
    /** Se descartó (vencido, roto, expuesto a temperatura fuera de rango…). */
    DISCARDED,
    /** Venció y sigue físicamente en el termo: no usar. Se cierra como DISCARDED. */
    EXPIRED;

    /** Estados de un lote que sigue en el termo. */
    public static final Set<LotStatus> IN_CONTAINER = EnumSet.of(ACTIVE, EXPIRED);

    public boolean isClosed() {
        return this == USED || this == DISCARDED;
    }
}
