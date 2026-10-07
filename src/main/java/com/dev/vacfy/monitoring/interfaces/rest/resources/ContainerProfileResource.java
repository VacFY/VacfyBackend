package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil asignado a un termo")
public record ContainerProfileResource(@Schema(example = "001") String contenedor, @Schema(example = "1") Long profileId) { }
