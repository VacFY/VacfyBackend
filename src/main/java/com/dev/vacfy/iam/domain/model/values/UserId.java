package com.dev.vacfy.iam.domain.model.values;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record UserId(UUID userId) {
    public UserId() { this(UUID.randomUUID()); }
}
