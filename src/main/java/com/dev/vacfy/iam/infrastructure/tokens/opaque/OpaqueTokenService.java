package com.dev.vacfy.iam.infrastructure.tokens.opaque;

import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.AuthorizationResponse;

import java.util.Optional;
import java.util.UUID;

public interface OpaqueTokenService {
    String generateToken(UUID userId);
    Optional<AuthorizationResponse> getUserDataFromToken(String token);
    void revokeToken(String token);
}