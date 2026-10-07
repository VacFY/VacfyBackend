package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Quién hace la consulta: su id y si es SUPERVISOR (ve todos los termos) o ENFERMERA (solo los suyos). */
public record Viewer(String userId, boolean supervisor) {
    public static final String SUPERVISOR_ROLE = "SUPERVISOR";

    /** A partir de los atributos que deja la sesión ("userId" y "userRole"). */
    public static Viewer of(Object userId, Object role) {
        return new Viewer(userId == null ? null : userId.toString(), role != null && SUPERVISOR_ROLE.equals(role.toString()));
    }
}
