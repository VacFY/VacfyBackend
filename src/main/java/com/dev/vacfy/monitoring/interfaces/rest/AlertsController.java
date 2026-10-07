package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import com.dev.vacfy.monitoring.domain.model.queries.GetAlertsQuery;
import com.dev.vacfy.monitoring.domain.services.MonitoringCommandService;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.AlertResource;
import com.dev.vacfy.monitoring.interfaces.rest.transform.MonitoringResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Alerts", description = "Alertas del motor de reglas")
public class AlertsController {
    private final MonitoringQueryService monitoringQueryService;
    private final MonitoringCommandService monitoringCommandService;

    public AlertsController(MonitoringQueryService monitoringQueryService, MonitoringCommandService monitoringCommandService) {
        this.monitoringQueryService = monitoringQueryService;
        this.monitoringCommandService = monitoringCommandService;
    }

    @GetMapping
    @Operation(summary = "Listar alertas", description = "status: OPEN (por defecto), ACTIVE, ACKNOWLEDGED, RESOLVED o ALL")
    public ResponseEntity<List<AlertResource>> getAlerts(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String contenedor) {
        try {
            var alerts = monitoringQueryService.handle(new GetAlertsQuery(status, contenedor));
            return ResponseEntity.ok(alerts.stream().map(MonitoringResourceAssembler::toResource).toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status inválido");
        }
    }

    @PatchMapping("/{alertId}/acknowledge")
    @Operation(summary = "Marcar alerta como vista", description = "La enfermera confirma que vio la alerta")
    public ResponseEntity<AlertResource> acknowledge(@PathVariable Long alertId, HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        var alert = monitoringCommandService.handle(new AcknowledgeAlertCommand(alertId, userId == null ? null : userId.toString()));
        return alert.map(MonitoringResourceAssembler::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
