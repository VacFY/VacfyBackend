package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record DeviceUserId(UUID userId) {
    public DeviceUserId() { this(null); }

    public DeviceUserId {
        if (userId == null) throw new IllegalArgumentException("Error retrieving the ID of the user");
    }
}
