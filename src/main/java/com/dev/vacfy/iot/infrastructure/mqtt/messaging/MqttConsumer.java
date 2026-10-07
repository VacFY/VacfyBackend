package com.dev.vacfy.iot.infrastructure.mqtt.messaging;

import com.dev.vacfy.iot.interfaces.websocket.WebSocketPublisher;
import com.dev.vacfy.iot.interfaces.websocket.resources.Telemetry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttConsumer {
    private final MqttAsyncClient client;
    private final WebSocketPublisher webSocketPublisher;
    private final ObjectMapper objectMapper;

    public MqttConsumer(WebSocketPublisher webSocketPublisher, ObjectMapper objectMapper) throws MqttException {
        this.webSocketPublisher = webSocketPublisher;
        this.objectMapper = objectMapper;

        String broker = "tcp://localhost:1883";
        String clientId = "backend-telemetry";
        String topic = "iot/telemetry";

        client = new MqttAsyncClient(broker, clientId);

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(false);

        client.setCallback(new MqttCallback() {

            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
                System.out.println("MQTT desconectado");
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
                exception.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                try {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

                    System.out.println("Topic: " + topic);
                    System.out.println("Payload: " + payload);

                    Telemetry telemetry = objectMapper.readValue(payload, Telemetry.class);

                    webSocketPublisher.iotWebSocketPublish(telemetry);

                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                System.out.println("Conectado a MQTT");

                try {
                    client.subscribe(topic, 1);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
            }
        });

        client.connect(options);
    }
}