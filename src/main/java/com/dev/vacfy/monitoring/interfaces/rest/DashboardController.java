package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.services.DashboardQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource;
import com.dev.vacfy.monitoring.interfaces.rest.transform.DashboardResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(value = "/api/v1/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dashboard", description = "Pantalla de inicio de web y móvil")
public class DashboardController {
    private final DashboardQueryService dashboardQueryService;

    public DashboardController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumen por termo",
            description = """
                    Un elemento por termo conocido (con telemetría reciente, perfil asignado, lotes o alertas), \
                    ordenado por código: temperatura actual, estado, rango vigilado, lotes, próximo vencimiento y \
                    alertas abiertas.

                    **Estados:** `SIN_DATOS` si nunca envió o lleva más de 2 min sin enviar (tiene prioridad), \
                    `ALERTA` si tiene alguna alerta abierta, `OK` en otro caso.

                    Para refrescar en vivo: vuelve a pedirlo cuando llegue un mensaje por `/ws/alerts`, o cada 30–60 s. \
                    La temperatura segundo a segundo llega por `/ws/device`.""")
    @ApiResponse(responseCode = "200", description = "Resumen de todos los termos")
    public ResponseEntity<DashboardSummaryResource> getSummary() {
        return ResponseEntity.ok(DashboardResourceAssembler.toResource(dashboardQueryService.getSummary(), Instant.now()));
    }
}
