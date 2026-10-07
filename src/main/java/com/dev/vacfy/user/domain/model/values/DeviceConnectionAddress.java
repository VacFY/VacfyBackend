package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

@Embeddable
public record DeviceConnectionAddress(String connectionAddress) {
    public DeviceConnectionAddress() { this(null); }

    public DeviceConnectionAddress {
        if (connectionAddress == null || connectionAddress.isBlank()) throw new IllegalArgumentException("Connection address empty");
    }
}
