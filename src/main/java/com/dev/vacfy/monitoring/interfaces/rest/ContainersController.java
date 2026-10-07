package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.commands.RegenerateContainerKeyCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterContainerCommand;
import com.dev.vacfy.monitoring.domain.services.ContainerCommandService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ApiErrorResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ContainerKeyResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.RegisterContainerResource;
import com.dev.vacfy.monitoring.interfaces.rest.transform.ContainerResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Containers", description = "Termos: registro con clave, vinculación a la enfermera que lo lleva y quién lo tuvo")
public class ContainersController {
    private final ContainerCommandService containerCommandService;

    public ContainersController(ContainerCommandService containerCommandService) {
        this.containerCommandService = containerCommandService;
    }

    @PostMapping(value = "/containers", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar un termo (supervisor)",
            description = """
                    Registra el código que envía el ESP32 y genera su **clave de vinculación** (formato `XXX-XXX`, sin \
                    caracteres que se confunden como 0/O o 1/I/L).

                    **La clave solo se muestra en esta respuesta**: imprímala en la etiqueta del termo. En la base de datos \
                    queda solo su hash. Si se pierde, use `regenerate-key`.

                    Si el termo ya enviaba datos antes de registrarse, se conservan sus lecturas y alertas.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"codigo\": \"001\", \"nombre\": \"Termo Posta Huambos\"}")))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Termo registrado; la clave viene en `clave`"),
            @ApiResponse(responseCode = "400", description = "Código inválido o nombre muy largo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "No es supervisor",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "El termo ya está registrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<ContainerKeyResource> registerContainer(@org.springframework.web.bind.annotation.RequestBody RegisterContainerResource resource,
                                                                  HttpServletRequest request) {
        var issued = containerCommandService.handle(new RegisterContainerCommand(RequestViewer.from(request),
                resource.codigo(), resource.nombre()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ContainerResourceAssembler.toResource(issued));
    }

    @PostMapping("/containers/{codigo}/regenerate-key")
    @Operation(summary = "Nueva clave para un termo (supervisor)",
            description = "Genera otra clave y la anterior deja de servir. La nueva solo se muestra en esta respuesta. "
                    + "Quien tenga el termo asignado lo sigue teniendo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clave nueva en `clave`"),
            @ApiResponse(responseCode = "403", description = "No es supervisor",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "El termo no está registrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<ContainerKeyResource> regenerateKey(@Parameter(description = "Código del termo", example = "001") @PathVariable String codigo,
                                                              HttpServletRequest request) {
        var issued = containerCommandService.handle(new RegenerateContainerKeyCommand(RequestViewer.from(request), codigo));
        return ResponseEntity.ok(ContainerResourceAssembler.toResource(issued));
    }
}
