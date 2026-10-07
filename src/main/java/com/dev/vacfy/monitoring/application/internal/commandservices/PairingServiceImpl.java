package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.Container;
import com.dev.vacfy.monitoring.domain.model.valueobjects.PairingCode;
import com.dev.vacfy.monitoring.domain.services.ContainerKeys;
import com.dev.vacfy.monitoring.domain.services.PairingService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Códigos temporales de 6 caracteres válidos unos minutos. Se guarda solo su hash, como la clave fija. */
@Service
public class PairingServiceImpl implements PairingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PairingServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ContainerRepository containerRepository;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public PairingServiceImpl(ContainerRepository containerRepository,
                              @Value("${vacty.pairing.code-ttl-seconds:300}") long ttlSeconds) {
        this(containerRepository, Duration.ofSeconds(ttlSeconds), Clock.systemUTC());
    }

    PairingServiceImpl(ContainerRepository containerRepository, Duration ttl, Clock clock) {
        this.containerRepository = containerRepository;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public Optional<PairingCode> issueTemporaryCode(String contenedor) {
        Optional<Container> container = containerRepository.findById(contenedor == null ? "" : contenedor.trim())
                .filter(Container::isActivo);
        if (container.isEmpty()) {
            LOGGER.debug("Código temporal no emitido: el termo {} no está registrado o está inactivo", contenedor);
            return Optional.empty();
        }
        String codigo = ContainerKeys.newTemporaryCode(RANDOM);
        Instant venceEn = clock.instant().plus(ttl);
        container.get().issueTemporaryCode(ContainerCommandServiceImpl.KEY_ENCODER.encode(codigo), venceEn);
        containerRepository.save(container.get());
        LOGGER.info("Código temporal emitido para el termo {} (vence {})", container.get().getCodigo(), venceEn);
        return Optional.of(new PairingCode(container.get().getCodigo(), codigo, venceEn));
    }
}
