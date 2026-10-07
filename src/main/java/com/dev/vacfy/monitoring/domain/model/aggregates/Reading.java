package com.dev.vacfy.monitoring.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/** Lectura de temperatura guardada (muestreada para no llenar la base de datos). */
@Entity
@Table(name = "readings", indexes = @Index(name = "idx_readings_contenedor_received_at", columnList = "contenedor, received_at"))
@Getter
public class Reading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String contenedor;

    @Column(nullable = false)
    private Double temperatura;

    private Double humedad;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected Reading() { }

    public Reading(String contenedor, Double temperatura, Double humedad, Instant receivedAt) {
        this.contenedor = contenedor;
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.receivedAt = receivedAt;
    }
}
