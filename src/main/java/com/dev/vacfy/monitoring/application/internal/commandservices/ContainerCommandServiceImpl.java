package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.domain.exceptions.ConflictException;
import com.dev.vacfy.monitoring.domain.exceptions.ForbiddenException;
import com.dev.vacfy.monitoring.domain.exceptions.InvalidDataException;
import com.dev.vacfy.monitoring.domain.exceptions.ResourceNotFoundException;
import com.dev.vacfy.monitoring.domain.model.aggregates.Container;
import com.dev.vacfy.monitoring.domain.model.commands.RegenerateContainerKeyCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterContainerCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.IssuedKey;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.domain.services.ContainerCommandService;
import com.dev.vacfy.monitoring.domain.services.ContainerKeys;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;

@Service
public class ContainerCommandServiceImpl implements ContainerCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerCommandServiceImpl.class);
    /** Las claves se guardan con BCrypt, igual que las contraseñas en iam. */
    static final PasswordEncoder KEY_ENCODER = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ContainerRepository containerRepository;
    private final Clock clock;

    public ContainerCommandServiceImpl(ContainerRepository containerRepository) {
        this(containerRepository, Clock.systemUTC());
    }

    ContainerCommandServiceImpl(ContainerRepository containerRepository, Clock clock) {
        this.containerRepository = containerRepository;
        this.clock = clock;
    }

    @Override
    public IssuedKey handle(RegisterContainerCommand command) {
        requireSupervisor(command.viewer(), "Solo el supervisor puede registrar termos.");
        String codigo = command.codigo() == null ? "" : command.codigo().trim();
        if (!ContainerKeys.isValidCodigo(codigo)) {
            throw new InvalidDataException("El código del termo admite de 1 a 32 letras, números, guion o guion bajo (p. ej. 001).");
        }
        String nombre = command.nombre() == null || command.nombre().isBlank() ? null : command.nombre().trim();
        if (nombre != null && nombre.length() > 100) throw new InvalidDataException("El nombre admite hasta 100 caracteres.");
        if (containerRepository.existsById(codigo)) {
            throw new ConflictException("El termo " + codigo + " ya está registrado. Para una clave nueva, use regenerate-key.");
        }
        String clave = ContainerKeys.newKey(RANDOM);
        Container container = containerRepository.save(new Container(codigo, nombre,
                KEY_ENCODER.encode(ContainerKeys.normalize(clave)), clock.instant()));
        LOGGER.info("Termo {} registrado", codigo);
        return new IssuedKey(container, clave);
    }

    @Override
    public IssuedKey handle(RegenerateContainerKeyCommand command) {
        requireSupervisor(command.viewer(), "Solo el supervisor puede cambiar la clave de un termo.");
        Container container = containerRepository.findById(command.codigo() == null ? "" : command.codigo().trim())
                .orElseThrow(() -> new ResourceNotFoundException("El termo " + command.codigo() + " no está registrado."));
        String clave = ContainerKeys.newKey(RANDOM);
        container.changeKey(KEY_ENCODER.encode(ContainerKeys.normalize(clave)));
        Container saved = containerRepository.save(container);
        LOGGER.info("Clave del termo {} regenerada", saved.getCodigo());
        return new IssuedKey(saved, clave);
    }

    static void requireSupervisor(Viewer viewer, String message) {
        if (viewer == null || !viewer.supervisor()) throw new ForbiddenException(message);
    }
}
