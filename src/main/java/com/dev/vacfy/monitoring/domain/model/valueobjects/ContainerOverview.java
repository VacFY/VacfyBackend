package com.dev.vacfy.monitoring.domain.model.valueobjects;

import com.dev.vacfy.monitoring.domain.model.aggregates.Container;

/**
 * Un termo en la vista del supervisor.
 *
 * @param container  null si el termo envía datos pero no está registrado
 * @param assignment asignación activa, o null si nadie lo tiene
 */
public record ContainerOverview(String contenedor, Container container, ContainerSummary summary, AssignmentView assignment) {
    public boolean registered() {
        return container != null;
    }
}
