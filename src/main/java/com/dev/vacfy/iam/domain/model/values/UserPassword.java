package com.dev.vacfy.iam.domain.model.values;

import jakarta.persistence.Embeddable;

@Embeddable
public record UserPassword(String userPassword) {
    public UserPassword() { this(null); }

    public UserPassword {
        if (userPassword == null || userPassword.isBlank()) throw new IllegalArgumentException("Password empty");
    }
}
