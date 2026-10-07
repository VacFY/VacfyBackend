package com.dev.vacfy.monitoring.domain.model.aggregates;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AssignmentEndReason;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Quién tiene un termo y desde cuándo. Un termo tiene como máximo una asignación activa (hasta = null),
 * garantizado por un índice único parcial (SchemaPatches). Una enfermera puede tener varios termos.
 */
@Entity
@Table(name = "container_assignments", indexes = {
        @Index(name = "idx_container_assignments_user_hasta", columnList = "user_id, hasta"),
        @Index(name = "idx_container_assignments_contenedor", columnList = "contenedor, desde")
})
@Getter
public class ContainerAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String contenedor;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false)
    private Instant desde;

    private Instant hasta;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_cierre", length = 32)
    private AssignmentEndReason motivoCierre;

    protected ContainerAssignment() { }

    public ContainerAssignment(String contenedor, String userId, Instant desde) {
        this.contenedor = contenedor;
        this.userId = userId;
        this.desde = desde;
    }

    public boolean isActive() {
        return hasta == null;
    }

    public void close(AssignmentEndReason reason, Instant at) {
        if (!isActive()) return;
        this.hasta = at;
        this.motivoCierre = reason;
    }
}
