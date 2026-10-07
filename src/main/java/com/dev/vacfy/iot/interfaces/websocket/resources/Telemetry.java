package com.dev.vacfy.iot.interfaces.websocket.resources;

public record Telemetry(String contenedor, Double temperatura, Double humedad, Double distancia) { }
