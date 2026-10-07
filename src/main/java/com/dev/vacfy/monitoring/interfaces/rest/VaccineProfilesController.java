package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.commands.AssignContainerProfileCommand;
import com.dev.vacfy.monitoring.domain.model.commands.CreateVaccineProfileCommand;
import com.dev.vacfy.monitoring.domain.services.MonitoringCommandService;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.AssignContainerProfileResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ContainerProfileResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.CreateVaccineProfileResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.VaccineProfileResource;
import com.dev.vacfy.monitoring.interfaces.rest.transform.MonitoringResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Vaccine profiles", description = "Rangos de temperatura por tipo de vacuna y contenedor")
public class VaccineProfilesController {
    private final MonitoringQueryService monitoringQueryService;
    private final MonitoringCommandService monitoringCommandService;

    public VaccineProfilesController(MonitoringQueryService monitoringQueryService, MonitoringCommandService monitoringCommandService) {
        this.monitoringQueryService = monitoringQueryService;
        this.monitoringCommandService = monitoringCommandService;
    }

    @GetMapping("/vaccine-profiles")
    @Operation(summary = "Listar perfiles de vacuna")
    public ResponseEntity<List<VaccineProfileResource>> getProfiles() {
        return ResponseEntity.ok(monitoringQueryService.getVaccineProfiles().stream()
                .map(MonitoringResourceAssembler::toResource).toList());
    }

    @PostMapping("/vaccine-profiles")
    @Operation(summary = "Crear perfil de vacuna", description = "Rango mínimo y máximo en °C; freezeSensitive=true si la vacuna se daña al congelarse")
    public ResponseEntity<VaccineProfileResource> createProfile(@RequestBody CreateVaccineProfileResource resource) {
        try {
            var profile = monitoringCommandService.handle(new CreateVaccineProfileCommand(
                    resource.name(), resource.minTemp(), resource.maxTemp(), resource.freezeSensitive()));
            return ResponseEntity.status(HttpStatus.CREATED).body(MonitoringResourceAssembler.toResource(profile));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/containers/{contenedor}/profile")
    @Operation(summary = "Asignar perfil a un contenedor", description = "contenedor = código que envía el ESP32 (p. ej. 001)")
    public ResponseEntity<ContainerProfileResource> assignProfile(@PathVariable String contenedor,
                                                                  @RequestBody AssignContainerProfileResource resource) {
        try {
            var saved = monitoringCommandService.handle(new AssignContainerProfileCommand(contenedor, resource.profileId()));
            return ResponseEntity.ok(MonitoringResourceAssembler.toResource(saved));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
