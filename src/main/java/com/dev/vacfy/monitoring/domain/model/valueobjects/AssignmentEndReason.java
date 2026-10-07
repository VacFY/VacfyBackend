package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Por qué terminó una asignación de termo. */
public enum AssignmentEndReason {
    /** La enfermera entregó el termo. */
    ENTREGADO,
    /** Otra enfermera se vinculó con la clave (cambio de turno). */
    TOMADO_POR_OTRA,
    /** El supervisor lo desvinculó. */
    DESVINCULADO_POR_SUPERVISOR
}
