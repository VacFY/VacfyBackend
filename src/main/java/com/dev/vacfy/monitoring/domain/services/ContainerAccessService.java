package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AccessScope;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;

import java.util.List;

/**
 * Único punto que decide qué termos ve cada usuario: SUPERVISOR todos, ENFERMERA solo los de sus asignaciones
 * activas. Usa un caché corto en memoria que se invalida al vincular o desvincular.
 */
public interface ContainerAccessService {
    AccessScope scope(Viewer viewer);

    boolean canSee(Viewer viewer, String contenedor);

    /** Termos de las asignaciones activas del usuario (para el supervisor, también solo los suyos). */
    List<String> visibleContainers(Viewer viewer);

    /** Olvida lo que se sabe de este usuario: la próxima consulta lee la base de datos. */
    void invalidate(String userId);
}
