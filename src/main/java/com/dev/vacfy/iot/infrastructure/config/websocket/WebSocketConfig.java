package com.dev.vacfy.iot.infrastructure.config.websocket;

import com.dev.vacfy.iot.interfaces.websocket.IotWebSocketHandler;
import com.dev.vacfy.monitoring.interfaces.websocket.AlertWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final IotWebSocketHandler iotWebSocketHandler;
    private final AlertWebSocketHandler alertWebSocketHandler;
    private final String[] allowedOrigins;

    public WebSocketConfig(IotWebSocketHandler iotWebSocketHandler,
                           AlertWebSocketHandler alertWebSocketHandler,
                           @Value("${vacty.allowed-origins}") String allowedOrigins) {
        this.iotWebSocketHandler = iotWebSocketHandler;
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.allowedOrigins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Telemetría en vivo (mismo payload que antes)
        registry.addHandler(iotWebSocketHandler, "/ws/device").setAllowedOriginPatterns(allowedOrigins);
        // Alertas del motor de reglas
        registry.addHandler(alertWebSocketHandler, "/ws/alerts").setAllowedOriginPatterns(allowedOrigins);
    }
}
