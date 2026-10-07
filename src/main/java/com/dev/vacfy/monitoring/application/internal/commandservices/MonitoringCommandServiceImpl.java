package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.application.internal.outboundservices.AlertPublisher;
import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerProfile;
import com.dev.vacfy.monitoring.domain.model.aggregates.Reading;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import com.dev.vacfy.monitoring.domain.model.commands.AssignContainerProfileCommand;
import com.dev.vacfy.monitoring.domain.model.commands.CreateVaccineProfileCommand;
import com.dev.vacfy.monitoring.domain.model.commands.ProcessTelemetryCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertEvent;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;
import com.dev.vacfy.monitoring.domain.services.AlertRuleEngine;
import com.dev.vacfy.monitoring.domain.services.ContainerMonitorState;
import com.dev.vacfy.monitoring.domain.services.MonitoringCommandService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerProfileRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ReadingRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitoringCommandServiceImpl implements MonitoringCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringCommandServiceImpl.class);
    private static final Duration LIMITS_CACHE_TTL = Duration.ofSeconds(60);

    /** Estado en memoria por contenedor (reglas + muestreo + caché del perfil). */
    private static final class MonitorEntry {
        final ContainerMonitorState state;
        Instant lastPersistedAt;
        TemperatureLimits limits;
        Instant limitsLoadedAt;

        MonitorEntry(ContainerMonitorState state) {
            this.state = state;
        }
    }

    private final AlertRuleEngine engine;
    private final ReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final VaccineProfileRepository vaccineProfileRepository;
    private final ContainerProfileRepository containerProfileRepository;
    private final AlertPublisher alertPublisher;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, MonitorEntry> entries = new ConcurrentHashMap<>();

    private final Duration persistEvery;
    private final TemperatureLimits fallbackLimits;

    public MonitoringCommandServiceImpl(AlertRuleEngine engine,
                                        ReadingRepository readingRepository,
                                        AlertRepository alertRepository,
                                        VaccineProfileRepository vaccineProfileRepository,
                                        ContainerProfileRepository containerProfileRepository,
                                        AlertPublisher alertPublisher,
                                        @Value("${vacty.readings.persist-every-seconds:30}") long persistEverySeconds,
                                        @Value("${vacty.alerts.default-profile-name}") String defaultProfileName,
                                        @Value("${vacty.alerts.default-min-temp:2.0}") double defaultMin,
                                        @Value("${vacty.alerts.default-max-temp:8.0}") double defaultMax) {
        this.engine = engine;
        this.readingRepository = readingRepository;
        this.alertRepository = alertRepository;
        this.vaccineProfileRepository = vaccineProfileRepository;
        this.containerProfileRepository = containerProfileRepository;
        this.alertPublisher = alertPublisher;
        this.persistEvery = Duration.ofSeconds(Math.max(0, persistEverySeconds));
        this.fallbackLimits = new TemperatureLimits(defaultProfileName, defaultMin, defaultMax, true);
    }

    /** Crea el perfil por defecto y empieza a vigilar los contenedores ya configurados. */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            if (!vaccineProfileRepository.existsByName(fallbackLimits.profileName())) {
                vaccineProfileRepository.save(new VaccineProfile(fallbackLimits.profileName(),
                        fallbackLimits.minTemp(), fallbackLimits.maxTemp(), fallbackLimits.freezeSensitive()));
                LOGGER.info("Perfil por defecto creado: {}", fallbackLimits.profileName());
            }
            Instant now = clock.instant();
            for (ContainerProfile containerProfile : containerProfileRepository.findAll()) {
                entry(containerProfile.getContenedor()).state.seedLastReadingAt(now);
            }
        } catch (Exception e) {
            LOGGER.error("No se pudo inicializar el módulo de monitoreo (¿base de datos disponible?)", e);
        }
    }

    @Override
    public void handle(ProcessTelemetryCommand command) {
        String contenedor = command.contenedor().trim();
        Instant now = clock.instant();
        MonitorEntry entry = entry(contenedor);

        synchronized (entry) {
            TemperatureLimits limits = limits(entry, contenedor, now);
            List<AlertEvent> events = engine.evaluate(entry.state, command.temperatura(), command.humedad(), now, limits);

            boolean valid = isValid(command.temperatura(), command.humedad());
            if (valid && shouldPersist(entry, now, events)) {
                readingRepository.save(new Reading(contenedor, command.temperatura(), command.humedad(), now));
                entry.lastPersistedAt = now;
            }
            if (valid && entry.state.hasOpenAlerts()) {
                trackOpenAlerts(contenedor, command.temperatura());
            }
            for (AlertEvent event : events) {
                apply(contenedor, event, now);
            }
        }
    }

    @Override
    public void checkOfflineContainers() {
        Instant now = clock.instant();
        entries.forEach((contenedor, entry) -> {
            synchronized (entry) {
                engine.checkOffline(entry.state, now).ifPresent(event -> apply(contenedor, event, now));
            }
        });
    }

    @Override
    public Optional<Alert> handle(AcknowledgeAlertCommand command) {
        return alertRepository.findById(command.alertId()).map(alert -> {
            alert.acknowledge(command.userId(), clock.instant());
            Alert saved = alertRepository.save(alert);
            alertPublisher.publish(saved);
            return saved;
        });
    }

    @Override
    public VaccineProfile handle(CreateVaccineProfileCommand command) {
        if (command.minTemp() == null || command.maxTemp() == null) {
            throw new IllegalArgumentException("minTemp y maxTemp son obligatorios");
        }
        if (command.name() != null && vaccineProfileRepository.existsByName(command.name().trim())) {
            throw new IllegalArgumentException("Ya existe un perfil con ese nombre");
        }
        return vaccineProfileRepository.save(new VaccineProfile(command.name(), command.minTemp(), command.maxTemp(),
                Boolean.TRUE.equals(command.freezeSensitive())));
    }

    @Override
    public ContainerProfile handle(AssignContainerProfileCommand command) {
        if (command.contenedor() == null || command.contenedor().isBlank()) {
            throw new IllegalArgumentException("El código del contenedor es obligatorio");
        }
        if (command.profileId() == null || !vaccineProfileRepository.existsById(command.profileId())) {
            throw new IllegalArgumentException("El perfil de vacuna no existe");
        }
        String contenedor = command.contenedor().trim();
        ContainerProfile containerProfile = containerProfileRepository.findById(contenedor)
                .map(existing -> {
                    existing.changeProfile(command.profileId());
                    return existing;
                })
                .orElseGet(() -> new ContainerProfile(contenedor, command.profileId()));
        ContainerProfile saved = containerProfileRepository.save(containerProfile);

        MonitorEntry entry = entry(contenedor);
        synchronized (entry) {
            entry.limitsLoadedAt = null; // fuerza recargar el rango en la próxima lectura
            entry.state.seedLastReadingAt(clock.instant());
        }
        return saved;
    }

    // ---------------------------------------------------------------------------------------------

    private MonitorEntry entry(String contenedor) {
        return entries.computeIfAbsent(contenedor, key -> {
            ContainerMonitorState state = new ContainerMonitorState(key);
            try {
                // Tras un reinicio, recupera las alertas que siguen abiertas en la base de datos
                state.restoreOpenAlerts(alertRepository.findByContenedorAndStatusIn(key, AlertStatus.OPEN)
                        .stream().map(Alert::getType).toList());
            } catch (Exception e) {
                LOGGER.warn("No se pudieron recuperar las alertas abiertas de {}", key, e);
            }
            return new MonitorEntry(state);
        });
    }

    private TemperatureLimits limits(MonitorEntry entry, String contenedor, Instant now) {
        if (entry.limits != null && entry.limitsLoadedAt != null
                && entry.limitsLoadedAt.plus(LIMITS_CACHE_TTL).isAfter(now)) {
            return entry.limits;
        }
        TemperatureLimits limits;
        try {
            limits = containerProfileRepository.findById(contenedor)
                    .flatMap(cp -> vaccineProfileRepository.findById(cp.getProfileId()))
                    .or(() -> vaccineProfileRepository.findByName(fallbackLimits.profileName()))
                    .map(VaccineProfile::toLimits)
                    .orElse(fallbackLimits);
        } catch (Exception e) {
            LOGGER.warn("No se pudo leer el perfil de {}; se usa {}", contenedor, fallbackLimits.profileName(), e);
            limits = entry.limits != null ? entry.limits : fallbackLimits;
        }
        entry.limits = limits;
        entry.limitsLoadedAt = now;
        return limits;
    }

    private boolean shouldPersist(MonitorEntry entry, Instant now, List<AlertEvent> events) {
        if (entry.lastPersistedAt == null || !events.isEmpty() || entry.state.hasOpenAlerts()) return true;
        return !entry.lastPersistedAt.plus(persistEvery).isAfter(now);
    }

    private boolean isValid(Double temperature, Double humidity) {
        var settings = engine.getSettings();
        boolean temperatureOk = temperature != null && !temperature.isNaN()
                && temperature >= settings.physicalMin() && temperature <= settings.physicalMax();
        boolean humidityOk = humidity == null || (!humidity.isNaN() && humidity >= 0 && humidity <= 100);
        return temperatureOk && humidityOk;
    }

    private void trackOpenAlerts(String contenedor, double temperature) {
        for (Alert alert : alertRepository.findByContenedorAndStatusIn(contenedor, AlertStatus.OPEN)) {
            Double min = alert.getMinValue();
            Double max = alert.getMaxValue();
            alert.track(temperature);
            if (!java.util.Objects.equals(min, alert.getMinValue()) || !java.util.Objects.equals(max, alert.getMaxValue())) {
                alertRepository.save(alert);
            }
        }
    }

    private void apply(String contenedor, AlertEvent event, Instant now) {
        Optional<Alert> open = alertRepository.findFirstByContenedorAndTypeAndStatusInOrderByStartedAtDesc(
                contenedor, event.type(), AlertStatus.OPEN);

        if (event.isOpen()) {
            if (open.isPresent()) return; // ya existe (p. ej. tras reiniciar el backend)
            Alert alert = alertRepository.save(new Alert(contenedor, event.type(), event.severity(),
                    event.value(), event.message(), now));
            LOGGER.warn("ALERTA {} [{}] contenedor {}: {}", alert.getType(), alert.getSeverity(), contenedor, alert.getMessage());
            alertPublisher.publish(alert);
        } else {
            open.ifPresent(alert -> {
                alert.resolve(event.message(), now);
                Alert saved = alertRepository.save(alert);
                LOGGER.info("Alerta {} resuelta en contenedor {}", saved.getType(), contenedor);
                alertPublisher.publish(saved);
            });
        }
    }
}
