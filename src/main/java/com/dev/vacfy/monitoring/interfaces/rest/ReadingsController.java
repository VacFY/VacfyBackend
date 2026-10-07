package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.queries.GetReadingsQuery;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ReadingResource;
import com.dev.vacfy.monitoring.interfaces.rest.transform.MonitoringResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Readings", description = "Historial de temperaturas")
public class ReadingsController {
    private final MonitoringQueryService monitoringQueryService;

    public ReadingsController(MonitoringQueryService monitoringQueryService) {
        this.monitoringQueryService = monitoringQueryService;
    }

    @GetMapping
    @Operation(summary = "Historial de lecturas", description = "from/to en ISO-8601 UTC (p. ej. 2026-10-07T00:00:00Z). Por defecto, últimas 24 h; máximo 1000.")
    public ResponseEntity<List<ReadingResource>> getReadings(@RequestParam String contenedor,
                                                             @RequestParam(required = false) String from,
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
