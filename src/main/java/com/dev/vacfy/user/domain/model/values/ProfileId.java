package com.dev.vacfy.user.domain.model.values;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record ProfileId(UUID profileId) {
    public ProfileId() { this(null); }

    public ProfileId {
        if (profileId == null) throw new IllegalArgumentException("Error retrieving the ID of the created user");
    }
}
