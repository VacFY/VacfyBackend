package com.dev.vacfy.user.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dispositivo del usuario")
public record DeviceResource(
        @Schema(description = "UUID; úsalo en DELETE /api/v1/device/{deviceId}", example = "8d0c6a1e-2b7f-4c1a-9f3e-5a6b7c8d9e0f") String deviceId,
        @Schema(example = "Termo 1") String deviceName,
        @Schema(example = "192.168.1.50") String deviceConnectionAddress) {
}
