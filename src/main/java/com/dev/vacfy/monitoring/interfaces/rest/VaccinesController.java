package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.commands.CreateVaccineCommand;
import com.dev.vacfy.monitoring.domain.model.commands.UpdateVaccineCommand;
import com.dev.vacfy.monitoring.domain.services.VaccineCommandService;
import com.dev.vacfy.monitoring.domain.services.VaccineQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ApiErrorResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.CreateVaccineResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.UpdateVaccineResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.VaccineResource;
import com.dev.vacfy.monitoring.interfaces.rest.transform.VaccineResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/vaccines", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Vaccines", description = "Catálogo de vacunas: para qué sirve cada una y en qué rango se conserva")
public class VaccinesController {
    private final VaccineCommandService vaccineCommandService;
    private final VaccineQueryService vaccineQueryService;

    public VaccinesController(VaccineCommandService vaccineCommandService, VaccineQueryService vaccineQueryService) {
        this.vaccineCommandService = vaccineCommandService;
        this.vaccineQueryService = vaccineQueryService;
    }

    @GetMapping
    @Operation(summary = "Listar vacunas",
            description = """
                    Catálogo ordenado por nombre. Úsalo para el selector de vacuna al registrar un lote \
                    (cuando el GTIN no está registrado o se llena a mano).

                    Al arrancar, el backend crea 10 vacunas del esquema nacional (2–8 °C, `verified=false`) \
                    si no existen. No incluye el perfil genérico "PAI estándar 2–8 °C", que sigue en `/vaccine-profiles`.""")
    @ApiResponse(responseCode = "200", description = "Lista de vacunas")
    public ResponseEntity<List<VaccineResource>> getVaccines() {
        return ResponseEntity.ok(vaccineQueryService.getVaccines().stream().map(VaccineResourceAssembler::toResource).toList());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear vacuna",
            description = "Solo `name` es obligatorio. Si no envías el rango, queda en 2–8 °C. La vacuna se crea con `verified=false`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vacuna creada"),
            @ApiResponse(responseCode = "400", description = "Falta el nombre, o el rango es inválido (mínimo ≥ máximo)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una vacuna con ese nombre",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<VaccineResource> createVaccine(@RequestBody CreateVaccineResource resource) {
        var vaccine = vaccineCommandService.handle(new CreateVaccineCommand(resource.name(), resource.protectsAgainst(),
                resource.minTemp(), resource.maxTemp(), resource.freezeSensitive(), resource.heatSensitive(),
                resource.dosesPerVial(), resource.notes()));
        return ResponseEntity.status(HttpStatus.CREATED).body(VaccineResourceAssembler.toResource(vaccine));
    }

    @PutMapping(value = "/{vaccineId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Corregir vacuna o marcarla como verificada",
            description = """
                    Actualiza solo los campos que envíes; los que falten (o vengan en null) se mantienen. \
                    Para marcarla como revisada basta con `{"verified": true}`.

                    Si cambias el rango, el termo que tenga lotes de esta vacuna recalcula su rango al instante. \
                    Responde 409 si el nuevo rango deja a algún termo sin un rango común con sus otros lotes.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vacuna actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (p. ej. mínimo ≥ máximo)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "La vacuna no existe",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Nombre repetido o rango incompatible con los lotes de algún termo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<VaccineResource> updateVaccine(@Parameter(description = "Id de la vacuna", example = "1") @PathVariable Long vaccineId,
                                                         @RequestBody UpdateVaccineResource resource) {
        var vaccine = vaccineCommandService.handle(new UpdateVaccineCommand(vaccineId, resource.name(),
                resource.protectsAgainst(), resource.minTemp(), resource.maxTemp(), resource.freezeSensitive(),
                resource.heatSensitive(), resource.dosesPerVial(), resource.notes(), resource.verified()));
        return ResponseEntity.ok(VaccineResourceAssembler.toResource(vaccine));
    }
}
