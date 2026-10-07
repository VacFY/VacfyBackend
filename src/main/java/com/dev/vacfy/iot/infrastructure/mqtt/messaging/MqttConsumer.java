package com.dev.vacfy.iot.infrastructure.mqtt.messaging;

import com.dev.vacfy.iot.interfaces.websocket.WebSocketPublisher;
import com.dev.vacfy.iot.interfaces.websocket.resources.Telemetry;
import com.dev.vacfy.monitoring.domain.model.commands.ProcessTelemetryCommand;
import com.dev.vacfy.monitoring.domain.services.MonitoringCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Recibe la telemetría del ESP32 por MQTT.
 * 1) La reenvía tal cual por /ws/device (igual que antes).
 * 2) La pasa al motor de alertas (módulo monitoring).
 */
@Component
public class MqttConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConsumer.class);
    /** "nan" no es JSON válido; el firmware antiguo lo envía cuando falla el DHT22. */
    private static final Pattern NAN_TOKEN = Pattern.compile("(?i)(?<=[:\\[,\\s])-?nan(?=\\s*[,}\\]])");

    private final WebSocketPublisher webSocketPublisher;
    private final MonitoringCommandService monitoringCommandService;
    private final ObjectMapper objectMapper;

    private final String brokerUrl;
    private final String username;
    private final String password;
    private final String topic;
    private final String clientId;
    private final long retrySeconds;

    private final ExecutorService processor = Executors.newSingleThreadExecutor(r -> new Thread(r, "telemetry-processor"));
    private final ScheduledExecutorService connector = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "mqtt-connector"));
    private MqttAsyncClient client;

    public MqttConsumer(WebSocketPublisher webSocketPublisher,
                        MonitoringCommandService monitoringCommandService,
                        ObjectMapper objectMapper,
                        @Value("${vacty.mqtt.broker-url}") String brokerUrl,
                        @Value("${vacty.mqtt.username:}") String username,
                        @Value("${vacty.mqtt.password:}") String password,
                        @Value("${vacty.mqtt.topic}") String topic,
                        @Value("${vacty.mqtt.client-id-prefix:vacty-backend}") String clientIdPrefix,
                        @Value("${vacty.mqtt.retry-seconds:10}") long retrySeconds) {
        this.webSocketPublisher = webSocketPublisher;
        this.monitoringCommandService = monitoringCommandService;
        this.objectMapper = objectMapper;
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
        this.topic = topic;
        // Sufijo aleatorio: dos instancias con el mismo clientId se desconectan entre sí
        this.clientId = clientIdPrefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.retrySeconds = retrySeconds;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        try {
            client = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());
            client.setCallback(new TelemetryCallback());
        } catch (MqttException e) {
            LOGGER.error("URL de broker MQTT inválida: {}", brokerUrl, e);
            return;
        }
        connector.execute(this::connect);
    }

    /** Paho solo reconecta solo si la primera conexión tuvo éxito; aquí reintentamos la primera. */
    private void connect() {
        if (client.isConnected()) return;
        try {
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);
            options.setConnectionTimeout(10);
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password == null ? new byte[0] : password.getBytes(StandardCharsets.UTF_8));
            }
            LOGGER.info("Conectando a MQTT {} como {}", brokerUrl, clientId);
            client.connect(options).waitForCompletion(15_000);
        } catch (MqttException e) {
            if (client.isConnected()) return; // conectó justo después del timeout
            LOGGER.warn("No se pudo conectar a MQTT ({}). Reintento en {} s", e.getMessage(), retrySeconds);
            if (!connector.isShutdown()) {
                connector.schedule(this::connect, retrySeconds, TimeUnit.SECONDS);
            }
        }
    }

    private void handlePayload(String payload) {
        Telemetry telemetry;
        try {
            String sanitized = NAN_TOKEN.matcher(payload).replaceAll("null");
            telemetry = objectMapper.readValue(sanitized, Telemetry.class);
        } catch (Exception e) {
            LOGGER.warn("Telemetría descartada (JSON inválido): {}", payload);
            return;
        }
        if (telemetry.contenedor() == null || telemetry.contenedor().isBlank()) {
            LOGGER.warn("Telemetría descartada (sin contenedor): {}", payload);
            return;
        }

        // 1) Panel en vivo: solo lecturas con temperatura (mismo payload que antes)
        if (telemetry.temperatura() != null && !telemetry.temperatura().isNaN()) {
            try {
                webSocketPublisher.iotWebSocketPublish(telemetry);
            } catch (Exception e) {
                LOGGER.warn("No se pudo publicar la telemetría por WebSocket", e);
            }
        }

        // 2) Motor de alertas
        try {
            monitoringCommandService.handle(new ProcessTelemetryCommand(
                    telemetry.contenedor(), telemetry.temperatura(), telemetry.humedad()));
        } catch (Exception e) {
            LOGGER.error("Error procesando la telemetría en el motor de alertas", e);
        }
    }

    @PreDestroy
    public void stop() {
        connector.shutdownNow();
        processor.shutdown();
        try {
            if (client != null && client.isConnected()) {
                client.disconnect().waitForCompletion(3_000);
            }
            if (client != null) {
                client.close();
            }
        } catch (MqttException e) {
            LOGGER.debug("Error cerrando MQTT", e);
        }
    }

    private class TelemetryCallback implements MqttCallback {
        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {
            LOGGER.warn("MQTT desconectado: {}", disconnectResponse);
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            LOGGER.error("Error MQTT", exception);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            LOGGER.debug("MQTT [{}] {}", topic, payload);
            // Fuera del hilo de Paho: el motor escribe en la base de datos
            if (!processor.isShutdown()) {
                processor.execute(() -> handlePayload(payload));
            }
        }

        @Override
        public void deliveryComplete(IMqttToken token) {
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            LOGGER.info("Conectado a MQTT {} (reconexión: {})", serverURI, reconnect);
            try {
                client.subscribe(topic, 1);
                LOGGER.info("Suscrito a {}", topic);
            } catch (MqttException e) {
                LOGGER.error("No se pudo suscribir a {}", topic, e);
            }
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
        }
    }
}
