package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record DeviceId(UUID deviceId) {
    public DeviceId() { this(UUID.randomUUID()); }
}
