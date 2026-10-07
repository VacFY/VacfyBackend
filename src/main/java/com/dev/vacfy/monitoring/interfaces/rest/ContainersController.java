package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.commands.LinkContainerCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegenerateContainerKeyCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterContainerCommand;
import com.dev.vacfy.monitoring.domain.model.commands.UnlinkContainerCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AssignmentView;
import com.dev.vacfy.monitoring.domain.services.ContainerCommandService;
import com.dev.vacfy.monitoring.domain.services.ContainerQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.*;
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

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Containers", description = "Termos: registro con clave, vinculación a la enfermera que lo lleva y quién lo tuvo")
public class ContainersController {
    private final ContainerCommandService containerCommandService;
    private final ContainerQueryService containerQueryService;

    public ContainersController(ContainerCommandService containerCommandService, ContainerQueryService containerQueryService) {
        this.containerCommandService = containerCommandService;
        this.containerQueryService = containerQueryService;
    }

    @GetMapping("/containers")
    @Operation(summary = "Todos los termos (supervisor)",
            description = """
                    Todos los termos, registrados o no, con quién tiene cada uno (`asignadoA`) y su estado, temperatura, \
                    rango, lotes y alertas (los mismos datos del dashboard).

                    `registrado: false` marca los termos que envían telemetría pero nadie registró: sus lecturas y alertas \
                    se guardan igual, pero nadie puede vincularlos hasta registrarlos con `POST /api/v1/containers`.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Termos ordenados por código"),
            @ApiResponse(responseCode = "403", description = "No es supervisor",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<List<ContainerOverviewResource>> getAllContainers(HttpServletRequest request) {
        return ResponseEntity.ok(containerQueryService.getAllContainers(RequestViewer.from(request)).stream()
                .map(ContainerResourceAssembler::toResource).toList());
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

    @PostMapping(value = "/containers/link", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Vincularme a un termo (código + clave)",
            description = """
                    La enfermera escribe el código y la clave de la etiqueta (o escanea el QR de la pantalla del termo, que \
                    trae `vacty:<codigo>:<clave>`). La clave se acepta en mayúsculas o minúsculas, con o sin guion y espacios.

                    - Si el termo lo tenía otra persona, esa asignación se cierra (`TOMADO_POR_OTRA`): es el cambio de turno.
                    - Si ya era suyo, responde 200 con `nuevaAsignacion: false` y no duplica nada.
                    - Se acepta la clave fija o un código temporal vigente (fase QR); el temporal sirve una sola vez.
                    - Si el código o la clave no coinciden, el mensaje no dice cuál de los dos falló.
                    - Tras 5 intentos fallidos en 10 minutos (por usuario o por termo) responde 429.

                    Al vincular, los afectados reciben `{"tipo":"ASIGNACION_CAMBIADA","contenedor":"001"}` por `/ws/alerts`.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = "Clave de la etiqueta", value = "{\"codigo\": \"001\", \"clave\": \"K7P-29Q\"}"),
            @ExampleObject(name = "Código temporal del QR", value = "{\"codigo\": \"001\", \"clave\": \"M4R8XZ\"}")
    }))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Termo vinculado (o ya era suyo)"),
            @ApiResponse(responseCode = "400", description = "Código o clave incorrectos, o vacíos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Otra persona se vinculó al mismo tiempo; reintentar",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos; esperar 10 minutos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<LinkResultResource> linkContainer(@org.springframework.web.bind.annotation.RequestBody LinkContainerResource resource,
                                                            HttpServletRequest request) {
        var result = containerCommandService.handle(new LinkContainerCommand(RequestViewer.from(request), resource.codigo(), resource.clave()));
        return ResponseEntity.ok(ContainerResourceAssembler.toResource(result));
    }

    @PostMapping("/containers/{codigo}/unlink")
    @Operation(summary = "Entregar un termo",
            description = "La enfermera deja el termo (`ENTREGADO`). El supervisor puede desvincular a cualquiera "
                    + "(`DESVINCULADO_POR_SUPERVISOR`). Otra enfermera recibe 403.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignación cerrada"),
            @ApiResponse(responseCode = "403", description = "El termo no es suyo y no es supervisor",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "El termo no está asignado a nadie",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<ContainerAssignmentResource> unlinkContainer(@Parameter(description = "Código del termo", example = "001") @PathVariable String codigo,
                                                                       HttpServletRequest request) {
        var closed = containerCommandService.handle(new UnlinkContainerCommand(RequestViewer.from(request), codigo));
        return ResponseEntity.ok(ContainerResourceAssembler.toResource(new AssignmentView(closed, null)));
    }

    @GetMapping("/my/containers")
    @Operation(summary = "Mis termos",
            description = """
                    Termos que la enfermera tiene ahora, con lo mismo que el dashboard: temperatura y humedad actuales, \
                    estado (`OK` / `ALERTA` / `SIN_DATOS`), rango vigilado, lotes, próximo vencimiento y alertas abiertas, \
                    más `asignadoDesde`. Lista vacía si no tiene ninguno.

                    Refréscala cuando llegue `ASIGNACION_CAMBIADA` por `/ws/alerts`.""")
    @ApiResponse(responseCode = "200", description = "Termos asignados")
    public ResponseEntity<List<MyContainerResource>> getMyContainers(HttpServletRequest request) {
        return ResponseEntity.ok(containerQueryService.getMyContainers(RequestViewer.from(request)).stream()
                .map(ContainerResourceAssembler::toResource).toList());
    }

    @GetMapping("/containers/{codigo}/assignments")
    @Operation(summary = "Quién tuvo el termo",
            description = "Historial de asignaciones, de la más reciente a la más antigua. Lo ve el supervisor, o la enfermera que tiene el termo ahora.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial (vacío si nadie lo tuvo)"),
            @ApiResponse(responseCode = "403", description = "No es supervisor ni tiene el termo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<List<ContainerAssignmentResource>> getAssignments(@Parameter(description = "Código del termo", example = "001") @PathVariable String codigo,
                                                                            HttpServletRequest request) {
        return ResponseEntity.ok(containerQueryService.getAssignments(RequestViewer.from(request), codigo).stream()
                .map(ContainerResourceAssembler::toResource).toList());
    }
}
