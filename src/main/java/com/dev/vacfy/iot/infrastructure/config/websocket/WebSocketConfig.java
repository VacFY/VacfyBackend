package com.dev.vacfy.iot.infrastructure.config.websocket;

import com.dev.vacfy.iam.infrastructure.authorization.sfs.websocket.SessionHandshakeInterceptor;
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
    private final SessionHandshakeInterceptor sessionHandshakeInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(IotWebSocketHandler iotWebSocketHandler,
                           AlertWebSocketHandler alertWebSocketHandler,
                           SessionHandshakeInterceptor sessionHandshakeInterceptor,
                           @Value("${vacty.allowed-origins}") String allowedOrigins) {
        this.iotWebSocketHandler = iotWebSocketHandler;
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.sessionHandshakeInterceptor = sessionHandshakeInterceptor;
        this.allowedOrigins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Telemetría en vivo (mismo payload que antes), solo de los termos que el usuario puede ver
        registry.addHandler(iotWebSocketHandler, "/ws/device")
                .addInterceptors(sessionHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
        // Alertas y avisos de asignación, filtrados igual
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .addInterceptors(sessionHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
