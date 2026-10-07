package com.dev.vacfy.monitoring.domain.model.commands;

public record ProcessTelemetryCommand(String contenedor, Double temperatura, Double humedad) { }
