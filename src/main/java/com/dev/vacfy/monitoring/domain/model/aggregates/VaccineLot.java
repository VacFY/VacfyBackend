package com.dev.vacfy.monitoring.domain.model.aggregates;

import com.dev.vacfy.monitoring.domain.model.valueobjects.LotRef;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotSource;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Lote de vacunas guardado en un termo. Un mismo lote de la misma vacuna solo puede estar
 * ACTIVE una vez por termo (índice único parcial creado en SchemaPatches).
 */
@Entity
@Table(name = "vaccine_lots", indexes = {
        @Index(name = "idx_vaccine_lots_contenedor_status", columnList = "contenedor, status"),
        @Index(name = "idx_vaccine_lots_expiry_date", columnList = "expiry_date")
})
@Getter
public class VaccineLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vaccine_id", nullable = false)
    private Long vaccineId;

    @Column(length = 14)
    private String gtin;

    @Column(name = "lot_number", nullable = false, length = 20)
    private String lotNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private Integer vials;

    private Integer doses;

    @Column(nullable = false, length = 64)
    private String contenedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LotSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LotStatus status;

    @Column(name = "registered_by", length = 64)
    private String registeredBy;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "close_reason", length = 300)
    private String closeReason;

    protected VaccineLot() { }

    public VaccineLot(Long vaccineId, String gtin, String lotNumber, LocalDate expiryDate, int vials, Integer doses,
                      String contenedor, LotSource source, String registeredBy, Instant registeredAt) {
        this.vaccineId = vaccineId;
        this.gtin = gtin;
        this.lotNumber = lotNumber;
        this.expiryDate = expiryDate;
        this.vials = vials;
        this.doses = doses;
        this.contenedor = contenedor;
        this.source = source;
        this.status = LotStatus.ACTIVE;
        this.registeredBy = registeredBy;
        this.registeredAt = registeredAt;
    }

    public long daysToExpiry(LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    /** Venció: sigue en el termo, pero ya no cuenta para el rango ni se debe usar. */
    public void markExpired() {
        if (status == LotStatus.ACTIVE) status = LotStatus.EXPIRED;
    }

    public void close(LotStatus newStatus, String reason, Instant at) {
        if (!newStatus.isClosed()) throw new IllegalArgumentException("Un lote se cierra como USED o DISCARDED");
        this.status = newStatus;
        this.closeReason = reason;
        this.closedAt = at;
    }

    public LotRef toRef(String vaccineName) {
        return new LotRef(id, contenedor, vaccineName, lotNumber, expiryDate);
    }
}
