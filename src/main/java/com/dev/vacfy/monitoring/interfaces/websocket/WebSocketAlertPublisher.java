package com.dev.vacfy.monitoring.interfaces.websocket;

import com.dev.vacfy.monitoring.application.internal.outboundservices.AlertPublisher;
import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.interfaces.rest.transform.MonitoringResourceAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Adaptador del puerto AlertPublisher: envía la alerta por /ws/alerts. */
@Service
public class WebSocketAlertPublisher implements AlertPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketAlertPublisher.class);
    private final AlertWebSocketHandler alertWebSocketHandler;
    private final ObjectMapper objectMapper;

    public WebSocketAlertPublisher(AlertWebSocketHandler alertWebSocketHandler, ObjectMapper objectMapper) {
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(Alert alert) {
        try {
            alertWebSocketHandler.sendToAll(objectMapper.writeValueAsString(MonitoringResourceAssembler.toResource(alert)));
        } catch (Exception e) {
            LOGGER.warn("No se pudo publicar la alerta {}", alert.getId(), e);
        }
    }
}
