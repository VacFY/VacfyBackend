package com.dev.vacfy.monitoring.application.internal.queryservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerProfile;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.valueobjects.*;
import com.dev.vacfy.monitoring.domain.services.DashboardQueryService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardQueryServiceImpl implements DashboardQueryService {
    /** Termos que enviaron datos en este periodo aparecen aunque no tengan perfil, lotes ni alertas. */
    private static final Duration RECENT_READINGS = Duration.ofHours(24);

    private final LiveTelemetryStore liveTelemetryStore;
    private final ContainerLimitsService containerLimitsService;
    private final ReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final VaccineLotRepository vaccineLotRepository;
    private final VaccineProfileRepository vaccineProfileRepository;
    private final ContainerProfileRepository containerProfileRepository;
    private final ZoneId zoneId;
    private final Duration offlineAfter;

    public DashboardQueryServiceImpl(LiveTelemetryStore liveTelemetryStore,
                                     ContainerLimitsService containerLimitsService,
                                     ReadingRepository readingRepository,
                                     AlertRepository alertRepository,
                                     VaccineLotRepository vaccineLotRepository,
                                     VaccineProfileRepository vaccineProfileRepository,
                                     ContainerProfileRepository containerProfileRepository,
                                     ZoneId vactyZoneId,
                                     @Value("${vacty.alerts.offline-after-seconds:120}") long offlineAfterSeconds) {
        this.liveTelemetryStore = liveTelemetryStore;
        this.containerLimitsService = containerLimitsService;
        this.readingRepository = readingRepository;
        this.alertRepository = alertRepository;
        this.vaccineLotRepository = vaccineLotRepository;
        this.vaccineProfileRepository = vaccineProfileRepository;
        this.containerProfileRepository = containerProfileRepository;
        this.zoneId = vactyZoneId;
        this.offlineAfter = Duration.ofSeconds(offlineAfterSeconds);
    }

    @Override
    public List<ContainerSummary> getSummary() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zoneId);

        Map<String, List<VaccineLot>> lotsByContainer = vaccineLotRepository
                .findByStatusInOrderByExpiryDateAsc(LotStatus.IN_CONTAINER).stream()
                .collect(Collectors.groupingBy(VaccineLot::getContenedor));
        Map<String, List<Alert>> alertsByContainer = alertRepository.findByStatusIn(AlertStatus.OPEN).stream()
                .collect(Collectors.groupingBy(Alert::getContenedor));
        Map<Long, VaccineProfile> vaccines = vaccineProfileRepository.findAll().stream()
                .collect(Collectors.toMap(VaccineProfile::getId, Function.identity()));

        SortedSet<String> containers = new TreeSet<>(liveTelemetryStore.containers());
        containerProfileRepository.findAll().stream().map(ContainerProfile::getContenedor).forEach(containers::add);
        containers.addAll(lotsByContainer.keySet());
        containers.addAll(alertsByContainer.keySet());
        containers.addAll(readingRepository.findContenedoresSince(now.minus(RECENT_READINGS)));

        List<ContainerSummary> summaries = new ArrayList<>();
        for (String contenedor : containers) {
            var live = liveTelemetryStore.lastValid(contenedor)
                    .or(() -> readingRepository.findFirstByContenedorOrderByReceivedAtDesc(contenedor)
                            .map(r -> new LiveTelemetryStore.LiveReading(r.getTemperatura(), r.getHumedad(), r.getReceivedAt())));
            Instant lastSeen = liveTelemetryStore.lastSeen(contenedor)
                    .orElse(live.map(LiveTelemetryStore.LiveReading::receivedAt).orElse(null));

            List<VaccineLot> lots = lotsByContainer.getOrDefault(contenedor, List.of());
            List<VaccineLot> active = lots.stream().filter(lot -> lot.getStatus() == LotStatus.ACTIVE).toList();
            LotView nextExpiry = active.isEmpty() ? null : new LotView(active.getFirst(),
                    vaccines.get(active.getFirst().getVaccineId()), active.getFirst().daysToExpiry(today));

            List<Alert> openAlerts = alertsByContainer.getOrDefault(contenedor, List.of());
            AlertSeverity highest = openAlerts.stream().map(Alert::getSeverity)
                    .max(Comparator.naturalOrder()).orElse(null);

            boolean noData = lastSeen == null || lastSeen.plus(offlineAfter).isBefore(now);
            ContainerStatus status = noData ? ContainerStatus.SIN_DATOS
                    : openAlerts.isEmpty() ? ContainerStatus.OK : ContainerStatus.ALERTA;

            summaries.add(new ContainerSummary(contenedor, status,
                    live.map(LiveTelemetryStore.LiveReading::temperatura).orElse(null),
                    live.map(LiveTelemetryStore.LiveReading::humedad).orElse(null),
                    live.map(LiveTelemetryStore.LiveReading::receivedAt).orElse(null),
                    containerLimitsService.get(contenedor), active.size(), lots.size() - active.size(),
                    nextExpiry, openAlerts.size(), highest));
        }
        return summaries;
    }
}
