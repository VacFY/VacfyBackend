package com.dev.vacfy.monitoring.domain.model.valueobjects;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Qué termos puede ver un usuario. SUPERVISOR: todos (all = true). ENFERMERA: los de sus asignaciones
 * activas, cada uno con la fecha desde la que lo tiene (para recortar el historial de lecturas).
 */
public record AccessScope(boolean all, Map<String, Instant> assignedSince) {
    private static final AccessScope EVERYTHING = new AccessScope(true, Map.of());
    private static final AccessScope NOTHING = new AccessScope(false, Map.of());

    public AccessScope {
        assignedSince = assignedSince == null ? Map.of() : Map.copyOf(assignedSince);
    }

    public static AccessScope everything() {
        return EVERYTHING;
    }

    public static AccessScope nothing() {
        return NOTHING;
    }

    public boolean canSee(String contenedor) {
        return all || (contenedor != null && assignedSince.containsKey(contenedor));
    }

    /** Desde cuándo puede ver el historial del termo; null = sin límite (supervisor). */
    public Instant since(String contenedor) {
        return all ? null : assignedSince.get(contenedor);
    }

    /** Termos asignados (vacío para el supervisor, que ve todos: usar all()). */
    public Set<String> containers() {
        return assignedSince.keySet();
    }
}
