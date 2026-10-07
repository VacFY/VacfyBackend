package com.dev.vacfy.user.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nuevo dispositivo del usuario")
public record CreateDeviceResource(
        @Schema(description = "Nombre visible", example = "Termo 1") String deviceName,
        @Schema(description = "Texto libre (IP, MAC…); el backend no lo usa", example = "192.168.1.50") String deviceConnectionAddress) {
}
