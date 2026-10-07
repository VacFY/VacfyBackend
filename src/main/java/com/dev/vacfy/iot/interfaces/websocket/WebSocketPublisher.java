package com.dev.vacfy.iot.interfaces.websocket;

import com.dev.vacfy.iot.interfaces.websocket.resources.Telemetry;

public interface WebSocketPublisher {
    void iotWebSocketPublish(Telemetry telemetry);
}
