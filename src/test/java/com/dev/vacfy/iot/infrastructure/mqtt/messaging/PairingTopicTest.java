package com.dev.vacfy.iot.infrastructure.mqtt.messaging;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PairingTopicTest {
    @Test
    void containerIsTakenFromThePairingTopic() {
        assertEquals(Optional.of("001"), MqttConsumer.containerFromPairingTopic("vacty/001/pairing/request"));
        assertEquals(Optional.empty(), MqttConsumer.containerFromPairingTopic("iot/telemetry"));
        assertEquals(Optional.empty(), MqttConsumer.containerFromPairingTopic("vacty/001/pairing/code"));
        assertEquals(Optional.empty(), MqttConsumer.containerFromPairingTopic("vacty/a/b/pairing/request"));
    }
}
