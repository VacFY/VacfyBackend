package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.commands.CloseLotCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterLotCommand;
import com.dev.vacfy.monitoring.domain.services.LotCommandService;
import com.dev.vacfy.monitoring.domain.services.LotQueryService;
import com.dev.vacfy.monitoring.interfaces.rest.resources.*;
import com.dev.vacfy.monitoring.interfaces.rest.transform.LotResourceAssembler;
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
@Tag(name = "Lots", description = "Lotes de vacunas en cada termo: leer el código de la caja, registrar, consultar y cerrar")
public class LotsController {
    private final LotCommandService lotCommandService;
    private final LotQueryService lotQueryService;

    public LotsController(LotCommandService lotCommandService, LotQueryService lotQueryService) {
        this.lotCommandService = lotCommandService;
        this.lotQueryService = lotQueryService;
    }

    @PostMapping(value = "/lots/read", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "1) Leer el código de una caja (no guarda)",
            description = """
                    Primer paso del registro. Recibe lo que dio el escáner o lo que escribió la enfermera y devuelve \
                    los datos para prellenar el formulario.

                    **Formatos aceptados**
                    - Texto crudo del escáner, con o sin prefijo `]d2`, y con el separador GS (ASCII 29, `\\u001d` en JSON) \
                    después del lote o la serie cuando no van al final. Si el escáner no puede enviar GS, se acepta `<GS>`.
                    - Formato legible escrito a mano: `(01)08901234567890(17)271231(10)AB1234`.
                    - Solo el GTIN (14 dígitos; también 8, 12 o 13, que se completan con ceros).

                    **AIs que se usan:** 01 GTIN (se valida el dígito verificador), 17 vencimiento AAMMDD (día 00 = último día \
                    del mes), 10 lote, 21 serie. Los demás se ignoran.

                    **Qué hacer con la respuesta**
                    - `knownProduct=true`: la vacuna ya viene en `vaccine`.
                    - `knownProduct=false`: muestra el selector de vacunas (GET /api/v1/vaccines). Al registrar, el GTIN \
                    quedará asociado a la vacuna elegida.
                    - Muestra `warnings` tal cual (vencido, vence en N días, falta el lote…).""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema = @Schema(implementation = ReadCodeResource.class),
            examples = {
                    @ExampleObject(name = "Escrito con paréntesis", value = "{\"code\": \"(01)08901234567890(17)271231(10)AB1234\"}"),
                    @ExampleObject(name = "Escaneado (crudo, con ]d2 y GS)", value = "{\"code\": \"]d2010890123456789017271231\\u001d10AB1234\"}"),
                    @ExampleObject(name = "Solo GTIN", value = "{\"code\": \"08901234567890\"}"),
                    @ExampleObject(name = "Dígito verificador inválido (400)", value = "{\"code\": \"(01)08901234567892(17)271231(10)AB1234\"}")
            }))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código leído"),
            @ApiResponse(responseCode = "400", description = "Código vacío, ilegible, GTIN con dígito verificador inválido o fecha imposible",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<CodeReadingResource> readCode(@org.springframework.web.bind.annotation.RequestBody ReadCodeResource resource) {
        return ResponseEntity.ok(LotResourceAssembler.toResource(lotQueryService.read(resource.code())));
    }

    @PostMapping(value = "/lots", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "2) Registrar un lote en un termo",
            description = """
                    Guarda el lote. Funciona con los datos de `/lots/read` o llenados a mano (sin GTIN).

                    **Rechaza con mensaje claro**
                    - 400 si la fecha ya pasó: una vacuna vencida no se registra. También si faltan datos.
                    - 409 si la vacuna necesita un rango que no tiene parte en común con los lotes que ya están en el termo \
                    (p. ej. una vacuna congelada junto a una de 2–8 °C).
                    - 409 si ese lote de esa vacuna ya está activo en el termo, o si el GTIN pertenece a otra vacuna.

                    **Efectos**
                    - Si el GTIN es nuevo, queda asociado a la vacuna para la próxima lectura.
                    - El termo recalcula su rango con todos sus lotes activos (mayor mínimo y menor máximo).
                    - Si vence en 30 días o menos, se abre al momento una alerta `LOT_EXPIRING`.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema = @Schema(implementation = RegisterLotResource.class),
            examples = {
                    @ExampleObject(name = "Escaneado", value = """
                            {"contenedor": "001", "vaccineId": 1, "lotNumber": "AB1234", "expiryDate": "2027-12-31",
                             "vials": 20, "doses": 200, "gtin": "08901234567890", "source": "SCAN"}"""),
                    @ExampleObject(name = "Manual, sin GTIN", value = """
                            {"contenedor": "001", "vaccineId": 2, "lotNumber": "HB77", "expiryDate": "2027-06-30",
                             "vials": 10, "source": "MANUAL"}""")
            }))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lote registrado"),
            @ApiResponse(responseCode = "400", description = "Faltan datos, fecha inválida o vencida, GTIN inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Rango incompatible con el termo, lote repetido o GTIN de otra vacuna",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<LotResource> registerLot(@org.springframework.web.bind.annotation.RequestBody RegisterLotResource resource,
                                                   HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        var lot = lotCommandService.handle(new RegisterLotCommand(resource.contenedor(), resource.vaccineId(),
                resource.lotNumber(), resource.expiryDate(), resource.vials(), resource.doses(), resource.gtin(),
                resource.source(), userId == null ? null : userId.toString()));
        return ResponseEntity.status(HttpStatus.CREATED).body(LotResourceAssembler.toResource(lot));
    }

    @GetMapping("/containers/{contenedor}/lots")
    @Operation(summary = "Lotes de un termo",
            description = "Lotes activos del termo, del que vence primero al último, con su vacuna, para qué sirve y `daysToExpiry`. "
                    + "Con `includeExpired=true` también devuelve los vencidos que siguen en el termo (para descartarlos).")
    @ApiResponse(responseCode = "200", description = "Lotes del termo (lista vacía si no tiene)")
    public ResponseEntity<List<LotResource>> getContainerLots(
            @Parameter(description = "Código del termo", example = "001") @PathVariable String contenedor,
            @Parameter(description = "Incluir los lotes vencidos (EXPIRED)") @RequestParam(defaultValue = "false") boolean includeExpired) {
        return ResponseEntity.ok(lotQueryService.getContainerLots(contenedor, includeExpired).stream()
                .map(LotResourceAssembler::toResource).toList());
    }

    @GetMapping("/lots/expiring")
    @Operation(summary = "Lotes por vencer",
            description = "Lotes activos de todos los termos que vencen en `days` días o menos, del más próximo al más lejano.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lotes por vencer"),
            @ApiResponse(responseCode = "400", description = "days fuera de 0–3650",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<List<LotResource>> getExpiringLots(
            @Parameter(description = "Ventana en días", example = "30") @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(lotQueryService.getExpiringLots(days).stream().map(LotResourceAssembler::toResource).toList());
    }

    @PatchMapping(value = "/lots/{lotId}/close", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cerrar un lote (usado o descartado)",
            description = """
                    Saca el lote del termo: `USED` si se terminó, `DISCARDED` si se descartó. Funciona con lotes ACTIVE y EXPIRED.

                    Al cerrarlo se resuelven sus alertas de vencimiento y el termo recalcula su rango sin este lote.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lote cerrado"),
            @ApiResponse(responseCode = "400", description = "status distinto de USED o DISCARDED",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "El lote no existe",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "El lote ya estaba cerrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResource.class)))
    })
    public ResponseEntity<LotResource> closeLot(@Parameter(description = "Id del lote", example = "7") @PathVariable Long lotId,
                                                @org.springframework.web.bind.annotation.RequestBody CloseLotResource resource) {
        var lot = lotCommandService.handle(new CloseLotCommand(lotId, resource.status(), resource.reason()));
        return ResponseEntity.ok(LotResourceAssembler.toResource(lot));
    }
}
