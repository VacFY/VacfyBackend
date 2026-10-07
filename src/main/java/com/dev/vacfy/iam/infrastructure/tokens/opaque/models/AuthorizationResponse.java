package com.dev.vacfy.iam.infrastructure.tokens.opaque.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorizationResponse(String userId) {
}
