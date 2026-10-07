package com.dev.vacfy.iam.infrastructure.tokens.opaque.models;

/** @param role ENFERMERA o SUPERVISOR */
public record AuthorizationResponse(String userId, String role) {
}
