package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.application.internal.queryservices.ContainerLimitsService;
import com.dev.vacfy.monitoring.domain.exceptions.ConflictException;
import com.dev.vacfy.monitoring.domain.exceptions.InvalidDataException;
import com.dev.vacfy.monitoring.domain.exceptions.ResourceNotFoundException;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.commands.CreateVaccineCommand;
import com.dev.vacfy.monitoring.domain.model.commands.UpdateVaccineCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.CareProfile;
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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class VaccineCommandServiceImpl implements VaccineCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaccineCommandServiceImpl.class);

    /**
     * Vacuna de la semilla. previousNames: nombres con los que la creó una semilla anterior.
     * Todas se conservan a 2–8 °C y quedan sin verificar (revisar con la ficha técnica del fabricante).
     */
    private record Seed(String name, List<String> previousNames, String protectsAgainst, boolean freezeSensitive,
                        boolean heatSensitive, CareProfile careProfile, List<String> careInstructions) { }

    private static final List<String> ANTI_FREEZE_CARE = List.of(
            "Se daña al congelarse.",
            "Usar paquetes fríos acondicionados.",
            "Evitar el contacto directo con el hielo.");

    private static final List<String> LIGHT_AND_HEAT_CARE = List.of(
            "No se daña al congelarse (revisar la nota del fabricante).",
            "Usar paquetes fríos acondicionados.",
            "Evitar el contacto directo con el hielo no es imprescindible para el vial.",
            "Proteger de la luz.");

    private static final List<Seed> SEED = List.of(
            antiFreeze("Pentavalente", List.of(), "Difteria, tos ferina, tétanos, hepatitis B y Haemophilus influenzae tipo b (Hib)"),
            antiFreeze("Hepatitis B", List.of(), "Hepatitis B"),
            antiFreeze("Neumococo", List.of(), "Neumonía, meningitis y otitis por neumococo"),
            antiFreeze("Polio inactivada (IPV)", List.of(), "Poliomielitis"),
            antiFreeze("Influenza inactivada", List.of("Influenza"), "Gripe estacional"),
            antiFreeze("VPH", List.of(), "Virus del papiloma humano"),
            new Seed("SPR", List.of(), "Sarampión, paperas y rubéola", false, true,
                    CareProfile.PROTECT_FROM_LIGHT_AND_HEAT, LIGHT_AND_HEAT_CARE),
            new Seed("Varicela", List.of(), "Varicela", false, true, CareProfile.CHECK_MANUFACTURER, List.of(
                    "Congelación: depende del producto.",
                    "Rango de 2 a 8 °C según el producto: verificar con el fabricante.",
                    "Usar paquetes fríos acondicionados.",
                    "Contacto directo con el hielo: según el producto.",
                    "Protección de la luz: según el producto.")),
            new Seed("BCG", List.of(), "Formas graves de tuberculosis", false, true,
                    CareProfile.PROTECT_FROM_LIGHT_AND_HEAT, LIGHT_AND_HEAT_CARE),
            new Seed("Rotavirus", List.of(), "Diarrea grave por rotavirus", false, true, CareProfile.CHECK_MANUFACTURER, List.of(
                    "Congelación: depende del producto.",
                    "Rango de 2 a 8 °C según el producto: verificar con el fabricante.",
                    "Usar paquetes fríos acondicionados.",
                    "Contacto directo con el hielo: según la formulación.",
                    "Protección de la luz: según el producto.")));

    private static Seed antiFreeze(String name, List<String> previousNames, String protectsAgainst) {
        return new Seed(name, previousNames, protectsAgainst, true, false, CareProfile.ANTI_FREEZE, ANTI_FREEZE_CARE);
    }

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

    /**
     * Crea las vacunas de la semilla que no existen. Las que creó una semilla anterior y nadie revisó todavía
     * (sin cuidados y sin verificar) se completan una sola vez: nombre, para qué sirve, sensibilidad y cuidados.
     * Conserva su rango, dosis por frasco y notas; las que ya tienen cuidados o están verificadas no se tocan.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedCatalog() {
        int created = 0;
        int updated = 0;
        for (Seed seed : SEED) {
            try {
                Optional<VaccineProfile> existing = vaccineProfileRepository.findByName(seed.name())
                        .or(() -> seed.previousNames().stream()
                                .map(vaccineProfileRepository::findByName)
                                .flatMap(Optional::stream)
                                .findFirst());
                if (existing.isEmpty()) {
                    VaccineProfile vaccine = new VaccineProfile(seed.name(), seed.protectsAgainst(),
                            VaccineProfile.DEFAULT_MIN_TEMP, VaccineProfile.DEFAULT_MAX_TEMP,
                            seed.freezeSensitive(), seed.heatSensitive(), null, null);
                    vaccine.updateCare(seed.careProfile(), seed.careInstructions());
                    vaccineProfileRepository.save(vaccine);
                    created++;
                } else if (existing.get().getCareProfile() == null && !existing.get().isVerified()) {
                    VaccineProfile vaccine = existing.get();
                    vaccine.update(seed.name(), seed.protectsAgainst(), vaccine.getMinTemp(), vaccine.getMaxTemp(),
                            seed.freezeSensitive(), seed.heatSensitive(), vaccine.getDosesPerVial(), vaccine.getNotes(), false);
                    vaccine.updateCare(seed.careProfile(), seed.careInstructions());
                    vaccineProfileRepository.save(vaccine);
                    updated++;
                }
            } catch (Exception e) {
                LOGGER.error("No se pudo crear o completar la vacuna {} de la semilla", seed.name(), e);
            }
        }
        if (updated > 0) containerLimitsService.invalidateAll(); // pudo cambiar la sensibilidad de algún termo
        if (created + updated > 0) LOGGER.info("Catálogo de vacunas: {} creadas, {} completadas", created, updated);
    }

    @Override
    public VaccineProfile handle(CreateVaccineCommand command) {
        if (command.name() != null && vaccineProfileRepository.existsByName(command.name().trim())) {
            throw new ConflictException("Ya existe una vacuna llamada \"" + command.name().trim() + "\"");
        }
        CareProfile careProfile = parseCareProfile(command.careProfile());
        try {
            VaccineProfile vaccine = new VaccineProfile(
                    command.name(),
                    command.protectsAgainst(),
                    orDefault(command.minTemp(), VaccineProfile.DEFAULT_MIN_TEMP),
                    orDefault(command.maxTemp(), VaccineProfile.DEFAULT_MAX_TEMP),
                    Boolean.TRUE.equals(command.freezeSensitive()),
                    Boolean.TRUE.equals(command.heatSensitive()),
                    command.dosesPerVial(),
                    command.notes());
            vaccine.updateCare(careProfile, command.careInstructions());
            return vaccineProfileRepository.save(vaccine);
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException(e.getMessage());
        }
    }

    @Override
    public VaccineProfile handle(UpdateVaccineCommand command) {
        VaccineProfile vaccine = vaccineProfileRepository.findById(command.vaccineId())
                .orElseThrow(() -> new ResourceNotFoundException("La vacuna " + command.vaccineId() + " no existe"));
        CareProfile careProfile = command.careProfile() != null ? parseCareProfile(command.careProfile()) : vaccine.getCareProfile();
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
            vaccine.updateCare(careProfile,
                    command.careInstructions() != null ? command.careInstructions() : vaccine.getCareInstructionList());
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

    private static CareProfile parseCareProfile(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return CareProfile.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("careProfile debe ser ANTI_FREEZE, PROTECT_FROM_LIGHT_AND_HEAT o CHECK_MANUFACTURER.");
        }
    }

    private static double orDefault(Double value, double fallback) {
        return value != null ? value : fallback;
    }
}
