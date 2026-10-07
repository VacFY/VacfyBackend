package com.dev.vacfy.monitoring.interfaces.websocket;

import com.dev.vacfy.monitoring.domain.model.queries.GetAlertsQuery;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.transform.MonitoringResourceAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /ws/alerts — cada mensaje es una alerta (AlertResource en JSON).
 * Al conectarse, el cliente recibe primero las alertas que siguen abiertas.
 * El campo "status" indica si está ACTIVE, ACKNOWLEDGED o RESOLVED.
 */
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final MonitoringQueryService monitoringQueryService;
    private final ObjectMapper objectMapper;

    public AlertWebSocketHandler(MonitoringQueryService monitoringQueryService, ObjectMapper objectMapper) {
        this.monitoringQueryService = monitoringQueryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        LOGGER.debug("Cliente de alertas conectado: {}", session.getId());
        try {
            for (var alert : monitoringQueryService.handle(new GetAlertsQuery("OPEN", null))) {
                send(session, objectMapper.writeValueAsString(MonitoringResourceAssembler.toResource(alert)));
            }
        } catch (Exception e) {
            LOGGER.warn("No se pudieron enviar las alertas abiertas a {}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        LOGGER.debug("Cliente de alertas desconectado: {}", session.getId());
    }

    public void sendToAll(String message) {
        sessions.forEach(session -> send(session, message));
    }

    /** A las sesiones de estos usuarios y a las de los supervisores. */
    public void sendToUsers(Set<String> userIds, String message) {
        sessions.forEach(session -> {
            Viewer viewer = viewerOf(session);
            if (viewer.supervisor() || userIds.contains(viewer.userId())) send(session, message);
        });
    }

    static Viewer viewerOf(WebSocketSession session) {
        return Viewer.of(session.getAttributes().get("userId"), session.getAttributes().get("userRole"));
    }

    private void send(WebSocketSession session, String message) {
        if (!session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            LOGGER.warn("No se pudo enviar la alerta a {}", session.getId(), e);
        }
    }
}
