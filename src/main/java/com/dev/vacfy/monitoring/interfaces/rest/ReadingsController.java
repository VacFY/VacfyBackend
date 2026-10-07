package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.queries.GetReadingsQuery;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ReadingResource;
import com.dev.vacfy.monitoring.interfaces.rest.transform.MonitoringResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/readings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Readings", description = "Historial de temperatura y humedad")
public class ReadingsController {
    private final MonitoringQueryService monitoringQueryService;

    public ReadingsController(MonitoringQueryService monitoringQueryService) {
        this.monitoringQueryService = monitoringQueryService;
    }

    @GetMapping
    @Operation(summary = "Historial de lecturas",
            description = "Lecturas guardadas de un termo, de la más reciente a la más antigua (invierte el arreglo para graficar). "
                    + "Se guarda 1 lectura cada 30 s (todas mientras hay una alerta abierta) y se devuelven como máximo 1000: "
                    + "unas 8 h. Para rangos más largos, pide tramos de 8 h o menos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lecturas"),
            @ApiResponse(responseCode = "400", description = "Falta contenedor o la fecha no es ISO-8601", content = @Content),
            @ApiResponse(responseCode = "401", description = "No hay sesión", content = @Content)
    })
    public ResponseEntity<List<ReadingResource>> getReadings(
            @Parameter(description = "Termo", example = "001") @RequestParam String contenedor,
            @Parameter(description = "Desde (ISO-8601 UTC). Por defecto, 24 h antes de `to`", example = "2026-10-07T00:00:00Z")
            @RequestParam(required = false) String from,
            @Parameter(description = "Hasta (ISO-8601 UTC). Por defecto, ahora", example = "2026-10-07T08:00:00Z")
            @RequestParam(required = false) String to) {
        try {
            var query = new GetReadingsQuery(contenedor,
                    from == null ? null : Instant.parse(from),
                    to == null ? null : Instant.parse(to));
            var readings = monitoringQueryService.handle(query);
            return ResponseEntity.ok(readings.stream().map(MonitoringResourceAssembler::toResource).toList());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha inválida: use ISO-8601, p. ej. 2026-10-07T00:00:00Z");
        }
    }
}
