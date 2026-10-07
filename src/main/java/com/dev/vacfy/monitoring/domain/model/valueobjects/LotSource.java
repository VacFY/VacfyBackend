package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Cómo se registró el lote. */
public enum LotSource {
    /** Escaneado con la cámara o un lector. */
    SCAN,
    /** Código GS1 escrito a mano. */
    TYPED_CODE,
    /** Datos llenados a mano, sin código. */
    MANUAL
}
