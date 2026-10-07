package com.dev.vacfy.iot.infrastructure.config.websocket;

import com.dev.vacfy.iot.interfaces.websocket.IotWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final IotWebSocketHandler iotWebSocketHandler;

    public WebSocketConfig(IotWebSocketHandler iotWebSocketHandler) {
        this.iotWebSocketHandler = iotWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(iotWebSocketHandler, "/ws/device");
    }
}
