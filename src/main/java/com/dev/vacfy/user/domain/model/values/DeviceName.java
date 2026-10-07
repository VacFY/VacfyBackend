package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

@Embeddable
public record DeviceName(String deviceName) {
    public DeviceName() { this(null); }

    public DeviceName {
        if (deviceName == null || deviceName.isBlank()) throw new IllegalArgumentException("Device name empty");
    }
}
