package com.dev.vacfy.monitoring.application.internal.queryservices;

import com.dev.vacfy.monitoring.domain.exceptions.InvalidDataException;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Gs1Data;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotCodeReading;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotView;
import com.dev.vacfy.monitoring.domain.services.Gs1Parser;
import com.dev.vacfy.monitoring.domain.services.LotQueryService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineLotRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProductRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LotQueryServiceImpl implements LotQueryService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_DAYS = 3650;

    private final VaccineLotRepository vaccineLotRepository;
    private final VaccineProfileRepository vaccineProfileRepository;
    private final VaccineProductRepository vaccineProductRepository;
    private final ZoneId zoneId;
    private final int expiringDays;

    public LotQueryServiceImpl(VaccineLotRepository vaccineLotRepository,
                               VaccineProfileRepository vaccineProfileRepository,
                               VaccineProductRepository vaccineProductRepository,
                               ZoneId vactyZoneId,
                               @Value("${vacty.alerts.expiring-days:30}") int expiringDays) {
        this.vaccineLotRepository = vaccineLotRepository;
        this.vaccineProfileRepository = vaccineProfileRepository;
        this.vaccineProductRepository = vaccineProductRepository;
        this.zoneId = vactyZoneId;
        this.expiringDays = expiringDays;
    }

    @Override
    public LotCodeReading read(String code) {
        Gs1Data data = Gs1Parser.parse(code);
        LocalDate today = LocalDate.now(zoneId);

        VaccineProfile vaccine = data.gtin() == null ? null : vaccineProductRepository.findByGtin(data.gtin())
                .flatMap(product -> vaccineProfileRepository.findById(product.getVaccineId()))
                .orElse(null);
        Long daysToExpiry = data.expiry() == null ? null : ChronoUnit.DAYS.between(today, data.expiry());

        List<String> warnings = new ArrayList<>();
        if (daysToExpiry == null) {
            warnings.add("Falta la fecha de vencimiento: escríbala.");
        } else if (daysToExpiry < 0) {
            warnings.add("Vencido el " + DATE.format(data.expiry()) + ": no se puede registrar.");
        } else if (daysToExpiry == 0) {
            warnings.add("Vence hoy.");
        } else if (daysToExpiry == 1) {
            warnings.add("Vence mañana.");
        } else if (daysToExpiry <= expiringDays) {
            warnings.add("Vence en " + daysToExpiry + " días.");
        }
        if (data.gtin() == null) {
            warnings.add("El código no trae GTIN: elija la vacuna.");
        } else if (vaccine == null) {
            warnings.add("GTIN no registrado: elija la vacuna.");
        }
        if (data.lot() == null) {
            warnings.add("Falta el número de lote: escríbalo.");
        }
        return new LotCodeReading(data, vaccine, vaccine != null, daysToExpiry, warnings);
    }

    @Override
    public List<LotView> getContainerLots(String contenedor, boolean includeExpired) {
        if (contenedor == null || contenedor.isBlank()) throw new InvalidDataException("Indique el termo (contenedor).");
        Set<LotStatus> statuses = includeExpired ? LotStatus.IN_CONTAINER : EnumSet.of(LotStatus.ACTIVE);
        return toViews(vaccineLotRepository.findByContenedorAndStatusInOrderByExpiryDateAsc(contenedor.trim(), statuses));
    }

    @Override
    public List<LotView> getExpiringLots(int days) {
        if (days < 0 || days > MAX_DAYS) throw new InvalidDataException("days debe estar entre 0 y " + MAX_DAYS + ".");
        LocalDate limit = LocalDate.now(zoneId).plusDays(days);
        return toViews(vaccineLotRepository.findByStatusAndExpiryDateLessThanEqualOrderByExpiryDateAsc(LotStatus.ACTIVE, limit));
    }

    private List<LotView> toViews(List<VaccineLot> lots) {
        LocalDate today = LocalDate.now(zoneId);
        Set<Long> vaccineIds = lots.stream().map(VaccineLot::getVaccineId).collect(Collectors.toSet());
        Map<Long, VaccineProfile> vaccines = vaccineProfileRepository.findAllById(vaccineIds).stream()
                .collect(Collectors.toMap(VaccineProfile::getId, Function.identity()));
        return lots.stream()
                .map(lot -> new LotView(lot, vaccines.get(lot.getVaccineId()), lot.daysToExpiry(today)))
                .toList();
    }
}
