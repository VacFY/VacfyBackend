package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.application.internal.outboundservices.AssignmentEventPublisher;
import com.dev.vacfy.monitoring.domain.exceptions.ConflictException;
import com.dev.vacfy.monitoring.domain.exceptions.ForbiddenException;
import com.dev.vacfy.monitoring.domain.exceptions.InvalidDataException;
import com.dev.vacfy.monitoring.domain.exceptions.ResourceNotFoundException;
import com.dev.vacfy.monitoring.domain.exceptions.TooManyRequestsException;
import com.dev.vacfy.monitoring.domain.model.aggregates.Container;
import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;
import com.dev.vacfy.monitoring.domain.model.commands.LinkContainerCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegenerateContainerKeyCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterContainerCommand;
import com.dev.vacfy.monitoring.domain.model.commands.UnlinkContainerCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AssignmentEndReason;
import com.dev.vacfy.monitoring.domain.model.valueobjects.IssuedKey;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LinkResult;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.domain.services.ContainerAccessService;
import com.dev.vacfy.monitoring.domain.services.ContainerCommandService;
import com.dev.vacfy.monitoring.domain.services.ContainerKeys;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerAssignmentRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContainerCommandServiceImpl implements ContainerCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerCommandServiceImpl.class);
    /** Las claves se guardan con BCrypt, igual que las contraseñas en iam. */
    static final PasswordEncoder KEY_ENCODER = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String WRONG_KEY = "Código o clave incorrectos.";

    private final ContainerRepository containerRepository;
    private final ContainerAssignmentRepository assignmentRepository;
    private final ContainerAccessService containerAccessService;
    private final LinkAttemptLimiter attemptLimiter;
    private final AssignmentEventPublisher assignmentEventPublisher;
    private final Clock clock;
    /** Un candado por termo: dos vinculaciones simultáneas al mismo termo se atienden una tras otra. */
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    @Autowired
    public ContainerCommandServiceImpl(ContainerRepository containerRepository,
                                       ContainerAssignmentRepository assignmentRepository,
                                       ContainerAccessService containerAccessService,
                                       LinkAttemptLimiter attemptLimiter,
                                       AssignmentEventPublisher assignmentEventPublisher) {
        this(containerRepository, assignmentRepository, containerAccessService, attemptLimiter, assignmentEventPublisher,
                Clock.systemUTC());
    }

    ContainerCommandServiceImpl(ContainerRepository containerRepository,
                                ContainerAssignmentRepository assignmentRepository,
                                ContainerAccessService containerAccessService,
                                LinkAttemptLimiter attemptLimiter,
                                AssignmentEventPublisher assignmentEventPublisher,
                                Clock clock) {
        this.containerRepository = containerRepository;
        this.assignmentRepository = assignmentRepository;
        this.containerAccessService = containerAccessService;
        this.attemptLimiter = attemptLimiter;
        this.assignmentEventPublisher = assignmentEventPublisher;
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

    @Override
    public LinkResult handle(LinkContainerCommand command) {
        Viewer viewer = command.viewer();
        if (viewer == null || viewer.userId() == null) throw new ForbiddenException("Inicie sesión para vincular un termo.");
        String codigo = command.codigo() == null ? "" : command.codigo().trim();
        String clave = ContainerKeys.normalize(command.clave());
        if (codigo.isEmpty() || clave.isEmpty()) throw new InvalidDataException("Escriba el código del termo y su clave.");

        String userKey = "user:" + viewer.userId();
        String codeKey = "code:" + codigo;
        if (attemptLimiter.isBlocked(userKey) || attemptLimiter.isBlocked(codeKey)) {
            throw new TooManyRequestsException("Demasiados intentos fallidos. Espere " + attemptLimiter.windowMinutes()
                    + " minutos e intente de nuevo.");
        }

        Instant now = clock.instant();
        Container container = containerRepository.findById(codigo).filter(Container::isActivo).orElse(null);
        boolean fixedKey = container != null && KEY_ENCODER.matches(clave, container.getClaveHash());
        boolean temporaryCode = !fixedKey && container != null && container.hasTemporaryCode(now)
                && KEY_ENCODER.matches(clave, container.getCodigoTemporalHash());
        if (!fixedKey && !temporaryCode) {
            // Mismo mensaje si falla el código o la clave: no revela qué termos existen
            attemptLimiter.recordFailure(userKey);
            attemptLimiter.recordFailure(codeKey);
            throw new InvalidDataException(WRONG_KEY);
        }
        attemptLimiter.reset(userKey);
        if (temporaryCode) {
            container.clearTemporaryCode(); // un código temporal sirve una sola vez
            containerRepository.save(container);
        }

        synchronized (locks.computeIfAbsent(codigo, key -> new Object())) {
            Optional<ContainerAssignment> current = assignmentRepository.findByContenedorAndHastaIsNull(codigo);
            if (current.isPresent() && current.get().getUserId().equals(viewer.userId())) {
                return new LinkResult(container, current.get(), false);
            }
            Set<String> affected = new LinkedHashSet<>();
            affected.add(viewer.userId());
            current.ifPresent(previous -> {
                previous.close(AssignmentEndReason.TOMADO_POR_OTRA, now);
                assignmentRepository.saveAndFlush(previous);
                affected.add(previous.getUserId());
            });
            ContainerAssignment created;
            try {
                created = assignmentRepository.saveAndFlush(new ContainerAssignment(codigo, viewer.userId(), now));
            } catch (DataIntegrityViolationException e) {
                throw new ConflictException("Otra persona se vinculó a este termo al mismo tiempo. Intente de nuevo.");
            }
            assignmentChanged(codigo, affected);
            LOGGER.info("Termo {} vinculado a {}{}", codigo, viewer.userId(),
                    current.map(previous -> " (antes lo tenía " + previous.getUserId() + ")").orElse(""));
            return new LinkResult(container, created, true);
        }
    }

    @Override
    public ContainerAssignment handle(UnlinkContainerCommand command) {
        Viewer viewer = command.viewer();
        String codigo = command.codigo() == null ? "" : command.codigo().trim();
        synchronized (locks.computeIfAbsent(codigo, key -> new Object())) {
            ContainerAssignment current = assignmentRepository.findByContenedorAndHastaIsNull(codigo)
                    .orElseThrow(() -> new ConflictException("El termo " + codigo + " no está asignado a nadie."));
            AssignmentEndReason reason;
            if (viewer != null && current.getUserId().equals(viewer.userId())) {
                reason = AssignmentEndReason.ENTREGADO;
            } else if (viewer != null && viewer.supervisor()) {
                reason = AssignmentEndReason.DESVINCULADO_POR_SUPERVISOR;
            } else {
                throw new ForbiddenException("El termo " + codigo + " no está asignado a usted.");
            }
            current.close(reason, clock.instant());
            ContainerAssignment saved = assignmentRepository.save(current);
            assignmentChanged(codigo, Set.of(saved.getUserId()));
            LOGGER.info("Termo {} desvinculado de {} ({})", codigo, saved.getUserId(), reason);
            return saved;
        }
    }

    private void assignmentChanged(String codigo, Set<String> userIds) {
        userIds.forEach(containerAccessService::invalidate);
        try {
            assignmentEventPublisher.assignmentChanged(codigo, userIds);
        } catch (Exception e) {
            LOGGER.warn("No se pudo avisar el cambio de asignación del termo {}", codigo, e);
        }
    }

    static void requireSupervisor(Viewer viewer, String message) {
        if (viewer == null || !viewer.supervisor()) throw new ForbiddenException(message);
    }
}
