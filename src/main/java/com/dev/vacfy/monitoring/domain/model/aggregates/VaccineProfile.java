package com.dev.vacfy.monitoring.domain.model.aggregates;

import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;
import jakarta.persistence.*;
import lombok.Getter;

/** Perfil de conservación: rango de temperatura según el tipo de vacuna. */
@Entity
@Table(name = "vaccine_profiles")
@Getter
public class VaccineProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "min_temp", nullable = false)
    private Double minTemp;

    @Column(name = "max_temp", nullable = false)
    private Double maxTemp;

    /** true si la vacuna se daña por congelación (p. ej. las que contienen adyuvante de aluminio). */
    @Column(name = "freeze_sensitive", nullable = false)
    private Boolean freezeSensitive;

    protected VaccineProfile() { }

    public VaccineProfile(String name, double minTemp, double maxTemp, boolean freezeSensitive) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("El nombre del perfil es obligatorio");
        new TemperatureLimits(name, minTemp, maxTemp, freezeSensitive); // valida el rango
        this.name = name.trim();
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.freezeSensitive = freezeSensitive;
    }

    public TemperatureLimits toLimits() {
        return new TemperatureLimits(name, minTemp, maxTemp, Boolean.TRUE.equals(freezeSensitive));
    }
}
