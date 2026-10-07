package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

@Embeddable
public record ProfileCompany(String profileCompany) {
    public ProfileCompany() { this(null); }

    public ProfileCompany {
        if (profileCompany == null || profileCompany.isBlank()) throw new IllegalArgumentException("Company empty");
    }
}
