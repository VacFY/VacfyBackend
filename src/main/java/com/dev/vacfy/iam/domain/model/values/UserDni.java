package com.dev.vacfy.iam.domain.model.values;

import jakarta.persistence.Embeddable;

@Embeddable
public record UserDni(String userDni) {
    public UserDni() { this(null); }

    public UserDni {
        if (userDni == null || userDni.isBlank()) throw new IllegalArgumentException("DNI empty");
    }
}
