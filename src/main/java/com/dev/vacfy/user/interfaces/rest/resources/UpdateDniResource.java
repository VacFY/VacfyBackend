package com.dev.vacfy.user.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DNI nuevo; también será el de inicio de sesión")
public record UpdateDniResource(@Schema(example = "87654321") String profileDni) {
}
