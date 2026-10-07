package com.dev.vacfy.monitoring.application.internal.queryservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotLimits;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;
import com.dev.vacfy.monitoring.domain.services.ContainerLimitsCalculator;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerProfileRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineLotRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rango de temperatura de cada termo, en este orden:
 *  1. sus lotes ACTIVE (mayor mínimo, menor máximo);
 *  2. sin lotes, el perfil asignado en container_profiles;
 *  3. si no, el perfil por defecto (2–8 °C).
 * Se guarda en caché 60 s; registrar o cerrar un lote, o editar una vacuna, la invalida.
 */
@Service
public class ContainerLimitsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerLimitsService.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private record Cached(TemperatureLimits limits, Instant loadedAt) { }

    private final VaccineLotRepository vaccineLotRepository;
    private final VaccineProfileRepository vaccineProfileRepository;
    private final ContainerProfileRepository containerProfileRepository;
    private final TemperatureLimits fallbackLimits;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public ContainerLimitsService(VaccineLotRepository vaccineLotRepository,
                                  VaccineProfileRepository vaccineProfileRepository,
                                  ContainerProfileRepository containerProfileRepository,
                                  @Value("${vacty.alerts.default-profile-name}") String defaultProfileName,
                                  @Value("${vacty.alerts.default-min-temp:2.0}") double defaultMin,
                                  @Value("${vacty.alerts.default-max-temp:8.0}") double defaultMax) {
        this.vaccineLotRepository = vaccineLotRepository;
        this.vaccineProfileRepository = vaccineProfileRepository;
        this.containerProfileRepository = containerProfileRepository;
        this.fallbackLimits = new TemperatureLimits(defaultProfileName, defaultMin, defaultMax, true);
    }

    /** Perfil por defecto (2–8 °C, sensible a congelación). */
    public TemperatureLimits fallbackLimits() {
        return fallbackLimits;
    }

    public TemperatureLimits get(String contenedor) {
        Instant now = Instant.now();
        Cached cached = cache.get(contenedor);
        if (cached != null && cached.loadedAt().plus(CACHE_TTL).isAfter(now)) {
            return cached.limits();
        }
        TemperatureLimits limits;
        try {
            limits = load(contenedor);
        } catch (Exception e) {
            LOGGER.warn("No se pudo leer el rango de {}; se usa el anterior o el de por defecto", contenedor, e);
            limits = cached != null ? cached.limits() : fallbackLimits;
        }
        cache.put(contenedor, new Cached(limits, now));
        return limits;
    }

    /** Fuerza recalcular el rango en la próxima lectura del termo. */
    public void invalidate(String contenedor) {
        cache.remove(contenedor);
    }

    public void invalidateAll() {
        cache.clear();
    }

    /** Lotes ACTIVE del termo con el rango de su vacuna (sin caché). */
    public List<LotLimits> activeLotLimits(String contenedor) {
        List<VaccineLot> lots = vaccineLotRepository.findByContenedorAndStatusInOrderByExpiryDateAsc(
                contenedor, EnumSet.of(LotStatus.ACTIVE));
        if (lots.isEmpty()) return List.of();
        Map<Long, VaccineProfile> vaccines = vaccineProfileRepository
                .findAllById(lots.stream().map(VaccineLot::getVaccineId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(VaccineProfile::getId, Function.identity()));
        return lots.stream()
                .filter(lot -> vaccines.containsKey(lot.getVaccineId()))
                .map(lot -> toLotLimits(lot, vaccines.get(lot.getVaccineId())))
                .toList();
    }

    public static LotLimits toLotLimits(VaccineLot lot, VaccineProfile vaccine) {
        return new LotLimits(lot.toRef(vaccine.getName()), vaccine.getMinTemp(), vaccine.getMaxTemp(),
                vaccine.isFreezeSensitive(), vaccine.isHeatSensitive());
    }

    private TemperatureLimits load(String contenedor) {
        List<LotLimits> lots = activeLotLimits(contenedor);
        if (!lots.isEmpty()) {
            try {
                return ContainerLimitsCalculator.combine(contenedor, lots);
            } catch (IllegalArgumentException e) {
                // No debería pasar: el registro de lotes y la edición de vacunas lo impiden
                LOGGER.error("Los lotes del termo {} no tienen un rango común; se usa su perfil", contenedor);
            }
        }
        return containerProfileRepository.findById(contenedor)
                .flatMap(containerProfile -> vaccineProfileRepository.findById(containerProfile.getProfileId()))
                .or(() -> vaccineProfileRepository.findByName(fallbackLimits.profileName()))
                .map(VaccineProfile::toLimits)
                .orElse(fallbackLimits);
    }
}
