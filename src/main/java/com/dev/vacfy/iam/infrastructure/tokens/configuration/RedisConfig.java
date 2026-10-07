package com.dev.vacfy.iam.infrastructure.tokens.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Ignora campos desconocidos: el firmware podrá agregar campos (id, ts) sin romper el backend
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
