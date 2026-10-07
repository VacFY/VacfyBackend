package com.dev.vacfy.iam.infrastructure.tokens.opaque.models;

import com.dev.vacfy.iam.domain.model.values.UserRole;

import java.time.Instant;
import java.util.UUID;

/** Sesión con el rol actual del usuario, leídos en una sola consulta. role es null en cuentas anteriores a los roles. */
public record SessionUser(UUID userId, Instant expiresAt, UserRole role) { }
