package com.dev.vacfy.monitoring.domain.model.valueobjects;

import com.dev.vacfy.monitoring.domain.model.aggregates.Container;
import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;

/** @param created false si el termo ya era de este usuario (no se duplica la asignación) */
public record LinkResult(Container container, ContainerAssignment assignment, boolean created) { }
