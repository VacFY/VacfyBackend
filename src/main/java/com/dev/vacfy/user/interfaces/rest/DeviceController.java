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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/device", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Device", description = "Device Endpoints")
public class DeviceController {
    private final DeviceCommandService deviceCommandService;
    private final DeviceQueryService deviceQueryService;

    public DeviceController(DeviceCommandService deviceCommandService, DeviceQueryService deviceQueryService) {
        this.deviceCommandService = deviceCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @GetMapping
    @Operation(summary = "Get a device", description = "Get a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device retrieved successfully."),
            @ApiResponse(responseCode = "404", description = "Device not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
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
    @Operation(summary = "Save a new device", description = "Save a new device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device saved successfully."),
            @ApiResponse(responseCode = "404", description = "Device not found.")
    })
    public ResponseEntity<Void> createDevice(@RequestBody CreateDeviceResource createDeviceResource, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var createDeviceCommand = CreateDeviceCommandFromResourceAssembler.toCommandFromResource(profileId, createDeviceResource);
        deviceCommandService.handle(createDeviceCommand);
        return ResponseEntity.ok().build();
    }

    @PutMapping
    @Operation(summary = "Update device information", description = "Update device information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device updated"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<Void> updateDevice(@RequestBody UpdateDeviceResource updateDeviceResource, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var updateDeviceCommand = UpdateDeviceCommandFromResourceAssembler.toCommandFromResource(profileId, updateDeviceResource);
        deviceCommandService.handle(updateDeviceCommand);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Delete device", description = "Delete device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device deleted"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<Void> updateDevice(@PathVariable String deviceId, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var deleteDeviceCommand = new DeleteDeviceCommand(profileId, deviceId);
        deviceCommandService.handle(deleteDeviceCommand);
        return ResponseEntity.ok().build();
    }
}
