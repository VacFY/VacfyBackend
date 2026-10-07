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

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Vaccine profiles", description = "Perfiles de rango (versión anterior del catálogo) y perfil asignado a cada termo")
public class VaccineProfilesController {
    private final MonitoringQueryService monitoringQueryService;
    private final MonitoringCommandService monitoringCommandService;

    public VaccineProfilesController(MonitoringQueryService monitoringQueryService, MonitoringCommandService monitoringCommandService) {
        this.monitoringQueryService = monitoringQueryService;
        this.monitoringCommandService = monitoringCommandService;
    }

    @GetMapping("/vaccine-profiles")
    @Operation(summary = "Listar perfiles de vacuna",
            description = "Misma tabla que /api/v1/vaccines, con menos campos; incluye el perfil por defecto \"PAI estándar 2–8 °C\". "
                    + "Para el catálogo nuevo usa /api/v1/vaccines.")
    @ApiResponse(responseCode = "200", description = "Perfiles ordenados por nombre")
    public ResponseEntity<List<VaccineProfileResource>> getProfiles() {
        return ResponseEntity.ok(monitoringQueryService.getVaccineProfiles().stream()
                .map(MonitoringResourceAssembler::toResource).toList());
    }

    @PostMapping("/vaccine-profiles")
    @Operation(summary = "Crear perfil de vacuna",
            description = "Rango mínimo y máximo en °C; freezeSensitive=true si la vacuna se daña al congelarse. "
                    + "Para crear vacunas con \"para qué sirve\" y sensibilidad al calor, usa POST /api/v1/vaccines.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"name\": \"Congelado -25 a -15\", \"minTemp\": -25.0, \"maxTemp\": -15.0, \"freezeSensitive\": false}")))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil creado"),
            @ApiResponse(responseCode = "400", description = "Falta el nombre o el rango, nombre repetido o mínimo ≥ máximo", content = @Content)
    })
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
    @Operation(summary = "Asignar perfil a un termo",
            description = "Define el rango de un termo **sin lotes**. Cuando el termo tiene lotes activos, su rango sale de "
                    + "esos lotes y este perfil se ignora hasta que se vacíe. Si ya tenía un perfil, lo reemplaza.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = "{\"profileId\": 1}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil asignado"),
            @ApiResponse(responseCode = "400", description = "El perfil no existe", content = @Content)
    })
    public ResponseEntity<ContainerProfileResource> assignProfile(@Parameter(description = "Código del termo (el que envía el ESP32)", example = "001") @PathVariable String contenedor,
                                                                  @RequestBody AssignContainerProfileResource resource) {
        try {
            var saved = monitoringCommandService.handle(new AssignContainerProfileCommand(contenedor, resource.profileId()));
            return ResponseEntity.ok(MonitoringResourceAssembler.toResource(saved));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
