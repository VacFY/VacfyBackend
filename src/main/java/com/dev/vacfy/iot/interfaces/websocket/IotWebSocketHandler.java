package com.dev.vacfy.iot.interfaces.websocket;

import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.domain.services.ContainerAccessService;
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

@Component
public class IotWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IotWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ContainerAccessService containerAccessService;

    public IotWebSocketHandler(ContainerAccessService containerAccessService) {
        this.containerAccessService = containerAccessService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        LOGGER.debug("Cliente conectado: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        LOGGER.debug("Mensaje recibido: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        LOGGER.debug("Cliente desconectado: {}", session.getId());
    }

    /** Telemetría de un termo: solo a las sesiones cuyo usuario puede verlo (supervisor, o la enfermera asignada). */
    public void sendToContainer(String contenedor, String message) {
        String codigo = contenedor == null ? null : contenedor.trim();
        sessions.forEach(session -> {
            if (!session.isOpen()) return;
            Viewer viewer = Viewer.of(session.getAttributes().get("userId"), session.getAttributes().get("userRole"));
            if (!containerAccessService.canSee(viewer, codigo)) return;
            try {
                // sendMessage no es seguro entre hilos para la misma sesión
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                LOGGER.warn("No se pudo enviar a {}", session.getId(), e);
            }
        });
    }
}
