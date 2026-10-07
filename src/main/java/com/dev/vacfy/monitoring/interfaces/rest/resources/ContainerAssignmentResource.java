package com.dev.vacfy.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Un periodo en que alguien tuvo el termo")
public record ContainerAssignmentResource(
        @Schema(example = "12") Long id,
        @Schema(example = "001") String contenedor,
        @Schema(example = "d5477e77-eab2-499c-bf91-6817865ac156") String userId,
        @Schema(description = "DNI y nombre de la persona (null si el usuario ya no existe)") AssigneeResource persona,
        @Schema(example = "2026-10-07T07:00:00Z") String desde,
        @Schema(description = "null mientras siga asignado", example = "2026-10-07T19:00:00Z") String hasta,
        @Schema(description = "null mientras siga asignado", allowableValues = {"ENTREGADO", "TOMADO_POR_OTRA", "DESVINCULADO_POR_SUPERVISOR"},
                example = "TOMADO_POR_OTRA") String motivoCierre) { }
