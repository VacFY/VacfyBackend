package com.dev.vacfy.monitoring.domain.model.aggregates;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertSeverity;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AlertType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/** Episodio de alerta: se abre una vez y se cierra cuando la condición termina. */
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_contenedor_status", columnList = "contenedor, status"),
        @Index(name = "idx_alerts_started_at", columnList = "started_at")
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

    protected Alert() { }

    public Alert(String contenedor, AlertType type, AlertSeverity severity, Double triggerValue, String message, Instant startedAt) {
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

    public void resolve(String resolutionMessage, Instant at) {
        if (!isOpen()) return;
        status = AlertStatus.RESOLVED;
        this.resolutionMessage = resolutionMessage;
        resolvedAt = at;
    }
}
