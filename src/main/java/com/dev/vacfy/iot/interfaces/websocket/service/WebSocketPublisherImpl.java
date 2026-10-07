package com.dev.vacfy.iot.interfaces.websocket.service;

import com.dev.vacfy.iot.interfaces.websocket.IotWebSocketHandler;
import com.dev.vacfy.iot.interfaces.websocket.WebSocketPublisher;
import com.dev.vacfy.iot.interfaces.websocket.resources.Telemetry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class WebSocketPublisherImpl implements WebSocketPublisher {
    private final IotWebSocketHandler iotWebSocketHandler;
    private final ObjectMapper objectMapper;

    public WebSocketPublisherImpl(IotWebSocketHandler iotWebSocketHandler, ObjectMapper objectMapper) {
        this.iotWebSocketHandler = iotWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public void iotWebSocketPublish(Telemetry telemetry) {
        try {
            String json = objectMapper.writeValueAsString(telemetry);
            iotWebSocketHandler.sendToAll(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing telemetry: ", e);
        }
    }
}
