package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

@Embeddable
public record ProfileDni(String profileDni) {
    public ProfileDni() { this("Undefined"); }

    public ProfileDni {
        if (profileDni == null || profileDni.isBlank()) throw new IllegalArgumentException("DNI empty");
    }
}
