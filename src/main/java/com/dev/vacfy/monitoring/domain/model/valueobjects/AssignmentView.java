package com.dev.vacfy.monitoring.domain.model.valueobjects;

import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;

/** Asignación con los datos de la persona (assignee puede ser null si el usuario ya no existe). */
public record AssignmentView(ContainerAssignment assignment, Assignee assignee) { }
