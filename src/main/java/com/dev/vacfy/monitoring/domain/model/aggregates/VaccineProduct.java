package com.dev.vacfy.monitoring.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Catálogo GTIN → vacuna. Se llena solo: cuando se registra un lote con un GTIN desconocido,
 * la vacuna que eligió la enfermera queda asociada para la próxima vez.
 */
@Entity
@Table(name = "vaccine_products")
@Getter
public class VaccineProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String gtin;

    @Column(name = "vaccine_id", nullable = false)
    private Long vaccineId;

    @Column(length = 200)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VaccineProduct() { }

    public VaccineProduct(String gtin, Long vaccineId, String description, Instant createdAt) {
        this.gtin = gtin;
        this.vaccineId = vaccineId;
        this.description = description;
        this.createdAt = createdAt;
    }
}
