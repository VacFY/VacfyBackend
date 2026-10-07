package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.application.internal.queryservices.ContainerLimitsService;
import com.dev.vacfy.monitoring.domain.exceptions.ConflictException;
import com.dev.vacfy.monitoring.domain.exceptions.InvalidDataException;
import com.dev.vacfy.monitoring.domain.exceptions.ResourceNotFoundException;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.commands.CreateVaccineCommand;
import com.dev.vacfy.monitoring.domain.model.commands.UpdateVaccineCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotLimits;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotStatus;
import com.dev.vacfy.monitoring.domain.services.ContainerLimitsCalculator;
import com.dev.vacfy.monitoring.domain.services.VaccineCommandService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineLotRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class VaccineCommandServiceImpl implements VaccineCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaccineCommandServiceImpl.class);

    private record Seed(String name, String protectsAgainst, boolean freezeSensitive, boolean heatSensitive) { }

    /** Semilla inicial: todas 2–8 °C y sin verificar (revisar con la ficha técnica del fabricante). */
    private static final List<Seed> SEED = List.of(
            new Seed("Pentavalente", "Difteria, tos ferina, tétanos, hepatitis B y Haemophilus influenzae tipo b (Hib)", true, false),
            new Seed("Hepatitis B", "Hepatitis B", true, false),
            new Seed("Neumococo", "Neumonía, meningitis y otitis por neumococo", true, false),
            new Seed("Polio inactivada (IPV)", "Poliomielitis", true, false),
            new Seed("Influenza", "Gripe estacional", true, false),
            new Seed("VPH", "Virus del papiloma humano", true, false),
            new Seed("SPR", "Sarampión, paperas y rubéola", false, true),
            new Seed("Varicela", "Varicela", false, true),
            new Seed("BCG", "Formas graves de tuberculosis", false, true),
            new Seed("Rotavirus", "Diarrea grave por rotavirus", false, true));

    private final VaccineProfileRepository vaccineProfileRepository;
    private final VaccineLotRepository vaccineLotRepository;
    private final ContainerLimitsService containerLimitsService;

    public VaccineCommandServiceImpl(VaccineProfileRepository vaccineProfileRepository,
                                     VaccineLotRepository vaccineLotRepository,
                                     ContainerLimitsService containerLimitsService) {
        this.vaccineProfileRepository = vaccineProfileRepository;
        this.vaccineLotRepository = vaccineLotRepository;
        this.containerLimitsService = containerLimitsService;
    }

    /** Crea las vacunas de la semilla que todavía no existen (por nombre). */
    @EventListener(ApplicationReadyEvent.class)
    public void seedCatalog() {
        try {
            int created = 0;
            for (Seed seed : SEED) {
                if (vaccineProfileRepository.existsByName(seed.name())) continue;
                vaccineProfileRepository.save(new VaccineProfile(seed.name(), seed.protectsAgainst(),
                        VaccineProfile.DEFAULT_MIN_TEMP, VaccineProfile.DEFAULT_MAX_TEMP,
                        seed.freezeSensitive(), seed.heatSensitive(), null, null));
                created++;
            }
            if (created > 0) LOGGER.info("Catálogo de vacunas: {} vacunas creadas", created);
        } catch (Exception e) {
            LOGGER.error("No se pudo crear el catálogo inicial de vacunas", e);
        }
    }

    @Override
    public VaccineProfile handle(CreateVaccineCommand command) {
        if (command.name() != null && vaccineProfileRepository.existsByName(command.name().trim())) {
            throw new ConflictException("Ya existe una vacuna llamada \"" + command.name().trim() + "\"");
        }
        try {
            return vaccineProfileRepository.save(new VaccineProfile(
                    command.name(),
                    command.protectsAgainst(),
                    orDefault(command.minTemp(), VaccineProfile.DEFAULT_MIN_TEMP),
                    orDefault(command.maxTemp(), VaccineProfile.DEFAULT_MAX_TEMP),
                    Boolean.TRUE.equals(command.freezeSensitive()),
                    Boolean.TRUE.equals(command.heatSensitive()),
                    command.dosesPerVial(),
                    command.notes()));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException(e.getMessage());
        }
    }

    @Override
    public VaccineProfile handle(UpdateVaccineCommand command) {
        VaccineProfile vaccine = vaccineProfileRepository.findById(command.vaccineId())
                .orElseThrow(() -> new ResourceNotFoundException("La vacuna " + command.vaccineId() + " no existe"));
        String name = command.name() != null ? command.name() : vaccine.getName();
        if (!name.trim().equals(vaccine.getName())) {
            vaccineProfileRepository.findByName(name.trim()).ifPresent(other -> {
                throw new ConflictException("Ya existe una vacuna llamada \"" + name.trim() + "\"");
            });
        }
        try {
            vaccine.update(
                    name,
                    command.protectsAgainst() != null ? command.protectsAgainst() : vaccine.getProtectsAgainst(),
                    orDefault(command.minTemp(), vaccine.getMinTemp()),
                    orDefault(command.maxTemp(), vaccine.getMaxTemp()),
                    command.freezeSensitive() != null ? command.freezeSensitive() : vaccine.isFreezeSensitive(),
                    command.heatSensitive() != null ? command.heatSensitive() : vaccine.isHeatSensitive(),
                    command.dosesPerVial() != null ? command.dosesPerVial() : vaccine.getDosesPerVial(),
                    command.notes() != null ? command.notes() : vaccine.getNotes(),
                    command.verified() != null ? command.verified() : vaccine.isVerified());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException(e.getMessage());
        }
        checkActiveLots(vaccine);
        VaccineProfile saved = vaccineProfileRepository.save(vaccine);
        containerLimitsService.invalidateAll(); // el rango o la sensibilidad de algún termo pudo cambiar
        return saved;
    }

    /** 409 si con el nuevo rango algún termo se queda sin un rango común entre sus lotes activos. */
    private void checkActiveLots(VaccineProfile updated) {
        Map<String, List<VaccineLot>> ownLotsByContainer = vaccineLotRepository
                .findByVaccineIdAndStatus(updated.getId(), LotStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(VaccineLot::getContenedor, TreeMap::new, Collectors.toList()));
        ownLotsByContainer.forEach((contenedor, ownLots) -> {
            Set<Long> ownIds = ownLots.stream().map(VaccineLot::getId).collect(Collectors.toSet());
            List<LotLimits> others = containerLimitsService.activeLotLimits(contenedor).stream()
                    .filter(lot -> !ownIds.contains(lot.lot().lotId()))
                    .toList();
            List<LotLimits> conflicts = ContainerLimitsCalculator.conflicts(
                    ContainerLimitsService.toLotLimits(ownLots.getFirst(), updated), others);
            if (!conflicts.isEmpty()) {
                throw new ConflictException("No se puede cambiar el rango de " + updated.getName() + " a "
                        + ContainerLimitsCalculator.range(updated.getMinTemp(), updated.getMaxTemp()) + ": en el termo "
                        + contenedor + " no tendría un rango común con "
                        + ContainerLimitsCalculator.describe(conflicts.getFirst()) + ".");
            }
        });
    }

    private static double orDefault(Double value, double fallback) {
        return value != null ? value : fallback;
    }
}
