package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import com.dev.vacfy.monitoring.domain.model.queries.GetAlertsQuery;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AccessScope;
import com.dev.vacfy.monitoring.domain.services.ContainerAccessService;
import com.dev.vacfy.monitoring.domain.services.MonitoringCommandService;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.AlertResource;
import com.dev.vacfy.monitoring.interfaces.rest.transform.MonitoringResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Alerts", description = "Alertas de temperatura y vencimiento generadas por el backend")
public class AlertsController {
    private final MonitoringQueryService monitoringQueryService;
    private final MonitoringCommandService monitoringCommandService;

    private final ContainerAccessService containerAccessService;

    public AlertsController(MonitoringQueryService monitoringQueryService, MonitoringCommandService monitoringCommandService,
                            ContainerAccessService containerAccessService) {
        this.monitoringQueryService = monitoringQueryService;
        this.monitoringCommandService = monitoringCommandService;
        this.containerAccessService = containerAccessService;
    }

    @GetMapping
    @Operation(summary = "Listar alertas",
            description = "Hasta 200 alertas, de la más reciente a la más antigua. Incluye las de temperatura "
                    + "(OUT_OF_RANGE, RAPID_CHANGE, SENSOR_OFFLINE, INVALID_READING) y las de vencimiento "
                    + "(LOT_EXPIRING, LOT_EXPIRED). Cada una trae title, message, severity y affectedLots listos para mostrar. "
                    + "Para recibirlas en vivo, conéctate a /ws/alerts (ver README). "
                    + "La enfermera solo ve las alertas de los termos que tiene asignados; el supervisor, todas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alertas"),
            @ApiResponse(responseCode = "400", description = "status inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "No hay sesión", content = @Content),
            @ApiResponse(responseCode = "403", description = "Pidió un contenedor que no tiene asignado", content = @Content)
    })
    public ResponseEntity<List<AlertResource>> getAlerts(
            @Parameter(description = "OPEN (por defecto: ACTIVE + ACKNOWLEDGED), ACTIVE, ACKNOWLEDGED, RESOLVED o ALL", example = "OPEN")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filtra por termo", example = "001")
            @RequestParam(required = false) String contenedor,
            HttpServletRequest request) {
        AccessScope scope = containerAccessService.scope(RequestViewer.from(request));
        if (contenedor != null && !contenedor.isBlank()) RequestViewer.requireAccess(scope, contenedor);
        try {
            var alerts = monitoringQueryService.handle(new GetAlertsQuery(status, contenedor, scope.all() ? null : scope.containers()));
            return ResponseEntity.ok(alerts.stream().map(MonitoringResourceAssembler::toResource).toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status inválido");
        }
    }

    @PatchMapping("/{alertId}/acknowledge")
    @Operation(summary = "Marcar alerta como vista",
            description = "La enfermera confirma que vio la alerta: pasa de ACTIVE a ACKNOWLEDGED y se envía por /ws/alerts. "
                    + "Si ya estaba vista o resuelta, la devuelve sin cambios. Una alerta de vencimiento que sube a CRITICAL "
                    + "vuelve a ACTIVE para que se vea otra vez.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta actualizada"),
            @ApiResponse(responseCode = "404", description = "La alerta no existe", content = @Content),
            @ApiResponse(responseCode = "401", description = "No hay sesión", content = @Content),
            @ApiResponse(responseCode = "403", description = "La alerta es de un termo que no tiene asignado", content = @Content)
    })
    public ResponseEntity<AlertResource> acknowledge(@Parameter(description = "Id de la alerta", example = "42") @PathVariable Long alertId,
                                                     HttpServletRequest request) {
        AccessScope scope = containerAccessService.scope(RequestViewer.from(request));
        monitoringQueryService.getAlert(alertId).ifPresent(alert -> RequestViewer.requireAccess(scope, alert.getContenedor()));
        Object userId = request.getAttribute("userId");
        var alert = monitoringCommandService.handle(new AcknowledgeAlertCommand(alertId, userId == null ? null : userId.toString()));
        return alert.map(MonitoringResourceAssembler::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
