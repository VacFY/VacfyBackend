package com.dev.vacfy.monitoring.domain.model.commands;

import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;

public record RegenerateContainerKeyCommand(Viewer viewer, String codigo) { }
