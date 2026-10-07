package com.dev.vacfy.user.interfaces.rest;

import com.dev.vacfy.user.domain.model.commands.DeleteDeviceCommand;
import com.dev.vacfy.user.domain.model.queries.GetDeviceByUserIdQuery;
import com.dev.vacfy.user.domain.services.DeviceCommandService;
import com.dev.vacfy.user.domain.services.DeviceQueryService;
import com.dev.vacfy.user.interfaces.rest.resources.CreateDeviceResource;
import com.dev.vacfy.user.interfaces.rest.resources.DeviceResource;
import com.dev.vacfy.user.interfaces.rest.resources.UpdateDeviceResource;
import com.dev.vacfy.user.interfaces.rest.transform.CreateDeviceCommandFromResourceAssembler;
import com.dev.vacfy.user.interfaces.rest.transform.DeviceResourceFromEntityAssembler;
import com.dev.vacfy.user.interfaces.rest.transform.UpdateDeviceCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/device", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Device", description = "Dispositivo del usuario")
public class DeviceController {
    private final DeviceCommandService deviceCommandService;
    private final DeviceQueryService deviceQueryService;

    public DeviceController(DeviceCommandService deviceCommandService, DeviceQueryService deviceQueryService) {
        this.deviceCommandService = deviceCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @GetMapping
    @Operation(summary = "Ver mi dispositivo",
            description = "Devuelve el dispositivo del usuario con sesión. Si responde 404, todavía no tiene uno: créalo con POST.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo del usuario"),
            @ApiResponse(responseCode = "404", description = "El usuario no tiene dispositivo", content = @Content),
            @ApiResponse(responseCode = "401", description = "No hay sesión", content = @Content),
            @ApiResponse(responseCode = "500", description = "El usuario tiene más de un dispositivo (ver POST)", content = @Content)
    })
    public ResponseEntity<DeviceResource> getDevice(HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var getDeviceByUserIdQuery = new GetDeviceByUserIdQuery(profileId);
        var device = deviceQueryService.handle(getDeviceByUserIdQuery);
        if (device.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var deviceResource = DeviceResourceFromEntityAssembler.toResourceFromEntity(device.get());
        return ResponseEntity.ok(deviceResource);
    }

    @PostMapping
    @Operation(summary = "Crear mi dispositivo",
            description = "Responde 200 sin cuerpo; para obtener el deviceId usa GET. "
                    + "Cada llamada crea un dispositivo nuevo y con dos GET y PUT fallan: llámalo solo cuando GET responda 404 "
                    + "y usa PUT para editar. Ambos campos son obligatorios.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"deviceName\": \"Termo 1\", \"deviceConnectionAddress\": \"192.168.1.50\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo creado"),
            @ApiResponse(responseCode = "401", description = "No hay sesión"),
            @ApiResponse(responseCode = "500", description = "Falta un campo o está vacío (todavía sin mensaje)")
    })
    public ResponseEntity<Void> createDevice(@RequestBody CreateDeviceResource createDeviceResource, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var createDeviceCommand = CreateDeviceCommandFromResourceAssembler.toCommandFromResource(profileId, createDeviceResource);
        deviceCommandService.handle(createDeviceCommand);
        return ResponseEntity.ok().build();
    }

    @PutMapping
    @Operation(summary = "Editar mi dispositivo", description = "Reemplaza nombre y dirección del dispositivo del usuario. Ambos campos son obligatorios.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"deviceName\": \"Termo 1 - Posta\", \"deviceConnectionAddress\": \"192.168.1.51\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo actualizado"),
            @ApiResponse(responseCode = "401", description = "No hay sesión"),
            @ApiResponse(responseCode = "500", description = "No tiene dispositivo o falta un campo (todavía sin mensaje)")
    })
    public ResponseEntity<Void> updateDevice(@RequestBody UpdateDeviceResource updateDeviceResource, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var updateDeviceCommand = UpdateDeviceCommandFromResourceAssembler.toCommandFromResource(profileId, updateDeviceResource);
        deviceCommandService.handle(updateDeviceCommand);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Borrar mi dispositivo", description = "deviceId es el UUID que devuelve GET /api/v1/device.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo borrado"),
            @ApiResponse(responseCode = "401", description = "No hay sesión"),
            @ApiResponse(responseCode = "500", description = "No existe, no es del usuario o no es un UUID (todavía sin mensaje)")
    })
    public ResponseEntity<Void> updateDevice(@PathVariable String deviceId, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var deleteDeviceCommand = new DeleteDeviceCommand(profileId, deviceId);
        deviceCommandService.handle(deleteDeviceCommand);
        return ResponseEntity.ok().build();
    }
}
