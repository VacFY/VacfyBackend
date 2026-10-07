package com.dev.vacfy.monitoring.interfaces.websocket;

import com.dev.vacfy.monitoring.application.internal.outboundservices.AssignmentEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

/** Adaptador del puerto AssignmentEventPublisher: avisa por /ws/alerts {"tipo":"ASIGNACION_CAMBIADA","contenedor":"001"}. */
@Service
public class WebSocketAssignmentEventPublisher implements AssignmentEventPublisher {
    private final AlertWebSocketHandler alertWebSocketHandler;

    public WebSocketAssignmentEventPublisher(AlertWebSocketHandler alertWebSocketHandler) {
        this.alertWebSocketHandler = alertWebSocketHandler;
    }

    @Override
    public void assignmentChanged(String contenedor, Collection<String> userIds) {
        String json = "{\"tipo\":\"ASIGNACION_CAMBIADA\",\"contenedor\":" + jsonString(contenedor) + "}";
        alertWebSocketHandler.sendToUsers(Set.copyOf(userIds), json);
    }

    /** El código del termo solo admite letras, números, - y _; aun así se escapa por seguridad. */
    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
