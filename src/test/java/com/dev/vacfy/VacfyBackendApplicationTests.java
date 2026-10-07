package com.dev.vacfy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Levanta todo el contexto: necesita Postgres, Redis y Mosquitto.
 * Se ejecuta solo con RUN_CONTEXT_TEST=true (p. ej. con docker compose levantado).
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_CONTEXT_TEST", matches = "true")
class VacfyBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
