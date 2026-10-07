package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

@Embeddable
public record ProfileName(String profileName) {
    public ProfileName() { this(null); }

    public ProfileName {
        if (profileName == null || profileName.isBlank()) throw new IllegalArgumentException("Name empty");
    }
}
