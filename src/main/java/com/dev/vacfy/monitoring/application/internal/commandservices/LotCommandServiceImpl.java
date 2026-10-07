package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.domain.exceptions.ConflictException;
import com.dev.vacfy.monitoring.domain.exceptions.InvalidDataException;
import com.dev.vacfy.monitoring.domain.exceptions.ResourceNotFoundException;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineLot;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProduct;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.commands.CloseLotCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterLotCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotSource;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotView;
import com.dev.vacfy.monitoring.domain.services.Gs1Parser;
import com.dev.vacfy.monitoring.domain.services.LotCommandService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineLotRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProductRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.VaccineProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
public class LotCommandServiceImpl implements LotCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LotCommandServiceImpl.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_LOT_LENGTH = 20;
    private static final int MAX_REASON_LENGTH = 300;

    private final VaccineLotRepository vaccineLotRepository;
    private final VaccineProfileRepository vaccineProfileRepository;
    private final VaccineProductRepository vaccineProductRepository;
    private final ZoneId zoneId;

    public LotCommandServiceImpl(VaccineLotRepository vaccineLotRepository,
                                 VaccineProfileRepository vaccineProfileRepository,
                                 VaccineProductRepository vaccineProductRepository,
                                 ZoneId vactyZoneId) {
        this.vaccineLotRepository = vaccineLotRepository;
        this.vaccineProfileRepository = vaccineProfileRepository;
        this.vaccineProductRepository = vaccineProductRepository;
        this.zoneId = vactyZoneId;
    }

    @Override
    public LotView handle(RegisterLotCommand command) {
        LocalDate today = LocalDate.now(zoneId);

        String contenedor = required(command.contenedor(), "Indique el termo (contenedor).");
        if (command.vaccineId() == null) throw new InvalidDataException("Elija la vacuna.");
        VaccineProfile vaccine = vaccineProfileRepository.findById(command.vaccineId())
                .orElseThrow(() -> new InvalidDataException("La vacuna " + command.vaccineId() + " no existe."));
        String lotNumber = required(command.lotNumber(), "Escriba el número de lote.").toUpperCase(Locale.ROOT);
        if (lotNumber.length() > MAX_LOT_LENGTH) {
            throw new InvalidDataException("El número de lote admite hasta " + MAX_LOT_LENGTH + " caracteres.");
        }
        LocalDate expiryDate = parseDate(command.expiryDate());
        if (expiryDate.isBefore(today)) {
            throw new InvalidDataException("El lote " + lotNumber + " venció el " + DATE.format(expiryDate)
                    + ": una vacuna vencida no se registra. Sepárela para su descarte.");
        }
        if (command.vials() == null || command.vials() < 1) throw new InvalidDataException("Indique cuántos frascos son (1 o más).");
        if (command.doses() != null && command.doses() < 1) throw new InvalidDataException("Las dosis deben ser 1 o más.");
        String gtin = normalizeGtin(command.gtin());
        LotSource source = parseSource(command.source());

        if (gtin != null) {
            vaccineProductRepository.findByGtin(gtin)
                    .filter(product -> !product.getVaccineId().equals(vaccine.getId()))
                    .ifPresent(product -> {
                        String registered = vaccineProfileRepository.findById(product.getVaccineId())
                                .map(VaccineProfile::getName).orElse("otra vacuna");
                        throw new ConflictException("El GTIN " + gtin + " corresponde a " + registered + ", no a "
                                + vaccine.getName() + ". Revise la vacuna elegida.");
                    });
        }
        if (vaccineLotRepository.existsByVaccineIdAndLotNumberAndContenedorAndStatus(vaccine.getId(), lotNumber, contenedor, LotStatus.ACTIVE)) {
            throw duplicateLot(lotNumber, vaccine, contenedor);
        }

        VaccineLot lot;
        try {
            lot = vaccineLotRepository.saveAndFlush(new VaccineLot(vaccine.getId(), gtin, lotNumber, expiryDate,
                    command.vials(), command.doses(), contenedor, source, command.registeredBy(), Instant.now()));
        } catch (DataIntegrityViolationException e) {
            throw duplicateLot(lotNumber, vaccine, contenedor); // otro registro simultáneo ganó
        }
        learnProduct(gtin, vaccine);
        LOGGER.info("Lote {} de {} registrado en el termo {}", lotNumber, vaccine.getName(), contenedor);
        return new LotView(lot, vaccine, lot.daysToExpiry(today));
    }

    @Override
    public LotView handle(CloseLotCommand command) {
        VaccineLot lot = vaccineLotRepository.findById(command.lotId())
                .orElseThrow(() -> new ResourceNotFoundException("El lote " + command.lotId() + " no existe."));
        LotStatus status = parseCloseStatus(command.status());
        if (lot.getStatus().isClosed()) {
            throw new ConflictException("El lote " + lot.getLotNumber() + " ya estaba cerrado como " + lot.getStatus() + ".");
        }
        String reason = command.reason() == null || command.reason().isBlank() ? null : command.reason().trim();
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidDataException("El motivo admite hasta " + MAX_REASON_LENGTH + " caracteres.");
        }
        lot.close(status, reason, Instant.now());
        VaccineLot saved = vaccineLotRepository.save(lot);
        VaccineProfile vaccine = vaccineProfileRepository.findById(saved.getVaccineId()).orElse(null);
        LOGGER.info("Lote {} del termo {} cerrado como {}", saved.getLotNumber(), saved.getContenedor(), status);
        return new LotView(saved, vaccine, saved.daysToExpiry(LocalDate.now(zoneId)));
    }

    // ---------------------------------------------------------------------------------------------

    /** Si el GTIN es nuevo, lo asocia a la vacuna elegida para reconocerlo la próxima vez. */
    private void learnProduct(String gtin, VaccineProfile vaccine) {
        if (gtin == null || vaccineProductRepository.findByGtin(gtin).isPresent()) return;
        try {
            vaccineProductRepository.save(new VaccineProduct(gtin, vaccine.getId(), vaccine.getName(), Instant.now()));
            LOGGER.info("GTIN {} asociado a {}", gtin, vaccine.getName());
        } catch (DataIntegrityViolationException e) {
            LOGGER.debug("El GTIN {} ya se había registrado", gtin);
        }
    }

    private static ConflictException duplicateLot(String lotNumber, VaccineProfile vaccine, String contenedor) {
        return new ConflictException("El lote " + lotNumber + " de " + vaccine.getName()
                + " ya está registrado en el termo " + contenedor + ".");
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new InvalidDataException(message);
        return value.trim();
    }

    private static LocalDate parseDate(String value) {
        String text = required(value, "Indique la fecha de vencimiento (AAAA-MM-DD).");
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidDataException("Fecha de vencimiento inválida: use AAAA-MM-DD, p. ej. 2027-12-31.");
        }
    }

    private static String normalizeGtin(String value) {
        if (value == null || value.isBlank()) return null;
        String digits = value.trim();
        if (!digits.matches("\\d{8}|\\d{12,14}")) throw new InvalidDataException("GTIN inválido: debe tener 14 dígitos.");
        String gtin = "0".repeat(14 - digits.length()) + digits;
        if (!Gs1Parser.isValidGtin(gtin)) {
            throw new InvalidDataException("GTIN inválido: " + gtin + " no pasa el dígito verificador.");
        }
        return gtin;
    }

    private static LotSource parseSource(String value) {
        if (value == null || value.isBlank()) return LotSource.MANUAL;
        try {
            return LotSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("source debe ser SCAN, TYPED_CODE o MANUAL.");
        }
    }

    private static LotStatus parseCloseStatus(String value) {
        try {
            LotStatus status = LotStatus.valueOf(required(value, "Indique status: USED o DISCARDED.").toUpperCase(Locale.ROOT));
            if (status.isClosed()) return status;
        } catch (IllegalArgumentException ignored) {
            // cae al mensaje de abajo
        }
        throw new InvalidDataException("status debe ser USED (se usó) o DISCARDED (se descartó).");
    }
}
