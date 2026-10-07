package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil que usará el termo mientras no tenga lotes")
public record AssignContainerProfileResource(@Schema(description = "Id de GET /api/v1/vaccine-profiles", example = "1") Long profileId) { }
