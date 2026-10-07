package com.dev.vacfy.monitoring.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;

/** Qué perfil de vacuna usa cada contenedor (código que envía el ESP32, p. ej. "001"). */
@Entity
@Table(name = "container_profiles")
@Getter
public class ContainerProfile {
    @Id
    @Column(length = 64)
    private String contenedor;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    protected ContainerProfile() { }

    public ContainerProfile(String contenedor, Long profileId) {
        this.contenedor = contenedor;
        this.profileId = profileId;
    }

    public void changeProfile(Long profileId) {
        this.profileId = profileId;
    }
}
