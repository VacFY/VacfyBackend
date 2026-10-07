package com.dev.vacfy.monitoring.interfaces.rest.resources;

public record ReadingResource(Long id, String contenedor, Double temperatura, Double humedad, String receivedAt) { }
