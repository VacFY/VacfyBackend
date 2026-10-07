package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

@Embeddable
public record ProfileLastName(String profileLastName) {
    public ProfileLastName() { this(null); }

    public ProfileLastName {
        if (profileLastName == null || profileLastName.isBlank()) throw new IllegalArgumentException("Last name empty");
    }
}
