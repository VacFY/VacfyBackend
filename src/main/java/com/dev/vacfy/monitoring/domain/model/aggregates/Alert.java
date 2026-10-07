package com.dev.vacfy.monitoring.domain.model.aggregates;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertSeverity;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AffectedLot;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.converters.AffectedLotsConverter;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/** Episodio de alerta: se abre una vez y se cierra cuando la condición termina. */
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_contenedor_status", columnList = "contenedor, status"),
        @Index(name = "idx_alerts_started_at", columnList = "started_at"),
        @Index(name = "idx_alerts_lot_id", columnList = "lot_id")
})
@Getter
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String contenedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status;

    @Column(length = 500)
    private String message;

    @Column(name = "trigger_value")
    private Double triggerValue;

    /** Temperatura mínima registrada durante el episodio. */
    @Column(name = "min_value")
    private Double minValue;

    /** Temperatura máxima registrada durante el episodio. */
    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by", length = 64)
    private String acknowledgedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_message", length = 500)
    private String resolutionMessage;

    /** Título corto para la notificación. null en alertas guardadas antes de existir el campo. */
    @Column(length = 160)
    private String title;

    /** Lotes en riesgo (vacuna + lote + vencimiento), guardados como JSON. */
    @Convert(converter = AffectedLotsConverter.class)
    @Column(name = "affected_lots", length = 4000)
    private List<AffectedLot> affectedLots;

    /** Lote al que se refiere una alerta de vencimiento (null en las de temperatura). */
    @Column(name = "lot_id")
    private Long lotId;

    protected Alert() { }

    public Alert(String contenedor, AlertType type, AlertSeverity severity, Double triggerValue, String message, Instant startedAt) {
        this(contenedor, type, severity, triggerValue, null, message, List.of(), null, startedAt);
    }

    public Alert(String contenedor, AlertType type, AlertSeverity severity, Double triggerValue, String title,
                 String message, List<AffectedLot> affectedLots, Long lotId, Instant startedAt) {
        this.title = title;
        this.affectedLots = affectedLots == null ? List.of() : List.copyOf(affectedLots);
        this.lotId = lotId;
        this.contenedor = contenedor;
        this.type = type;
        this.severity = severity;
        this.status = AlertStatus.ACTIVE;
        this.triggerValue = triggerValue;
        this.minValue = triggerValue;
        this.maxValue = triggerValue;
        this.message = message;
        this.startedAt = startedAt;
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    /** Registra la temperatura durante el episodio (para saber el peor valor alcanzado). */
    public void track(Double temperature) {
        if (temperature == null || temperature.isNaN()) return;
        minValue = minValue == null ? temperature : Math.min(minValue, temperature);
        maxValue = maxValue == null ? temperature : Math.max(maxValue, temperature);
    }

    public void acknowledge(String userId, Instant at) {
        if (status != AlertStatus.ACTIVE) return;
        status = AlertStatus.ACKNOWLEDGED;
        acknowledgedBy = userId;
        acknowledgedAt = at;
    }

    /**
     * Sube la severidad (p. ej. un lote que pasa a vencer en 7 días o menos). Vuelve a ACTIVE para que
     * alguien la vea de nuevo.
     */
    public void escalate(AlertSeverity severity, String title, String message) {
        if (!isOpen()) return;
        this.severity = severity;
        if (title != null) this.title = title;
        if (message != null) this.message = message;
        this.status = AlertStatus.ACTIVE;
        this.acknowledgedAt = null;
        this.acknowledgedBy = null;
    }

    public void resolve(String resolutionMessage, Instant at) {
        if (!isOpen()) return;
        status = AlertStatus.RESOLVED;
        this.resolutionMessage = resolutionMessage;
        resolvedAt = at;
    }
}
