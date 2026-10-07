package com.dev.vacfy.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ticket de un solo uso para abrir /ws/device o /ws/alerts con ?ticket=")
public record WebSocketTicketResource(
        @Schema(example = "q3VbM1k6m0Zr8yQ2cX7pL0aWnE4tJ9uH") String ticket,
        @Schema(description = "Segundos que dura si no se usa", example = "60") long expiresInSeconds) {
}
