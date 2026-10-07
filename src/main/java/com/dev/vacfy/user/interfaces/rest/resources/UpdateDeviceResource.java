package com.dev.vacfy.user.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos nuevos del dispositivo (ambos obligatorios)")
public record UpdateDeviceResource(
        @Schema(example = "Termo 1 - Posta") String deviceName,
        @Schema(example = "192.168.1.51") String deviceConnectionAddress) {
}
