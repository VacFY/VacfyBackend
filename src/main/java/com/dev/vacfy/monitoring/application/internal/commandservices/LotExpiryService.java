package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.application.internal.outboundservices.AlertPublisher;
import com.dev.vacfy.monitoring.application.internal.queryservices.ContainerLimitsService;
import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertEvent;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertSeverity;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotStatus;
import com.dev.vacfy.monitoring.domain.services.LotExpiryRules;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineLotRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Aplica LotExpiryRules: abre, escala y cierra las alertas de vencimiento y marca los lotes vencidos. */
@Service
public class LotExpiryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LotExpiryService.class);

    private final LotExpiryRules rules;
    private final VaccineLotRepository vaccineLotRepository;
    private final VaccineProfileRepository vaccineProfileRepository;
    private final AlertRepository alertRepository;
    private final AlertPublisher alertPublisher;
    private final ContainerLimitsService containerLimitsService;
    private final ZoneId zoneId;

    public LotExpiryService(LotExpiryRules rules,
                            VaccineLotRepository vaccineLotRepository,
                            VaccineProfileRepository vaccineProfileRepository,
                            AlertRepository alertRepository,
                            AlertPublisher alertPublisher,
                            ContainerLimitsService containerLimitsService,
                            ZoneId vactyZoneId) {
        this.rules = rules;
        this.vaccineLotRepository = vaccineLotRepository;
        this.vaccineProfileRepository = vaccineProfileRepository;
        this.alertRepository = alertRepository;
        this.alertPublisher = alertPublisher;
        this.containerLimitsService = containerLimitsService;
        this.zoneId = vactyZoneId;
    }

    /** Revisión diaria: todos los lotes ACTIVE que vencen dentro de la ventana de aviso (o ya vencieron). */
    public void checkAll() {
        LocalDate today = LocalDate.now(zoneId);
        List<VaccineLot> lots = vaccineLotRepository.findByStatusAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
                LotStatus.ACTIVE, today.plusDays(rules.getExpiringDays()));
        Map<Long, String> names = vaccineProfileRepository
                .findAllById(lots.stream().map(VaccineLot::getVaccineId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(VaccineProfile::getId, VaccineProfile::getName));
        for (VaccineLot lot : lots) {
            try {
                check(lot, names.getOrDefault(lot.getVaccineId(), "vacuna " + lot.getVaccineId()), today);
            } catch (Exception e) {
                LOGGER.error("No se pudo revisar el vencimiento del lote {}", lot.getId(), e);
            }
        }
        LOGGER.info("Vencimientos revisados: {} lotes en la ventana de {} días", lots.size(), rules.getExpiringDays());
    }

    /** Revisa un lote (al registrarlo y en la revisión diaria). */
    public void check(VaccineLot lot, String vaccineName, LocalDate today) {
        if (lot.getStatus() != LotStatus.ACTIVE) return;
        List<Alert> open = alertRepository.findByLotIdAndStatusIn(lot.getId(), AlertStatus.OPEN);
        Optional<Alert> expiring = find(open, AlertType.LOT_EXPIRING);
        boolean expiredOpen = find(open, AlertType.LOT_EXPIRED).isPresent();

        List<AlertEvent> events = rules.evaluate(lot.toRef(vaccineName), today,
                expiring.map(Alert::getSeverity).orElse(null), expiredOpen);
        Instant now = Instant.now();
        for (AlertEvent event : events) {
            switch (event.action()) {
                case OPEN -> {
                    Alert alert = alertRepository.save(new Alert(lot.getContenedor(), event.type(), event.severity(), null,
                            event.title(), event.message(), event.affectedLots(), lot.getId(), now));
                    LOGGER.warn("ALERTA {} [{}] lote {} termo {}", alert.getType(), alert.getSeverity(),
                            lot.getLotNumber(), lot.getContenedor());
                    alertPublisher.publish(alert);
                }
                case ESCALATE -> expiring.ifPresent(alert -> {
                    alert.escalate(event.severity(), event.title(), event.message());
                    alertPublisher.publish(alertRepository.save(alert));
                });
                case RESOLVE -> find(open, event.type()).ifPresent(alert -> {
                    alert.resolve(event.message(), now);
                    alertPublisher.publish(alertRepository.save(alert));
                });
            }
        }

        if (rules.stage(lot.getExpiryDate(), today) == LotExpiryRules.Stage.EXPIRED) {
            lot.markExpired();
            vaccineLotRepository.save(lot);
            containerLimitsService.invalidate(lot.getContenedor()); // un lote vencido ya no cuenta para el rango
            LOGGER.warn("Lote {} de {} vencido en el termo {}", lot.getLotNumber(), vaccineName, lot.getContenedor());
        }
    }

    /** Al cerrar un lote (USED o DISCARDED) se cierran sus alertas de vencimiento. */
    public void resolveForClosedLot(VaccineLot lot) {
        Instant now = Instant.now();
        String reason = lot.getCloseReason() == null ? "" : ": " + lot.getCloseReason();
        String message = (lot.getStatus() == LotStatus.USED ? "Lote usado" : "Lote descartado") + reason + ".";
        for (Alert alert : alertRepository.findByLotIdAndStatusIn(lot.getId(), AlertStatus.OPEN)) {
            alert.resolve(message, now);
            alertPublisher.publish(alertRepository.save(alert));
        }
    }

    private static Optional<Alert> find(List<Alert> alerts, AlertType type) {
        return alerts.stream().filter(alert -> alert.getType() == type).findFirst();
    }
}
