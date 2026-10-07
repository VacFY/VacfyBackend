package com.dev.vacfy.iam.infrastructure.authorization.sfs.websocket;

import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.AuthorizationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tickets de un solo uso para abrir /ws/device y /ws/alerts sin cookie. El panel web desplegado llama a la
 * API por el proxy de su hosting (la cookie queda en su dominio) y abre el WebSocket directo contra el backend,
 * así que pide un ticket con su sesión y lo envía en {@code ?ticket=}.
 * Viven en memoria: con una sola instancia del backend es suficiente; con varias, habría que compartirlos.
 */
@Component
public class WebSocketTicketService {
    public static final Duration TTL = Duration.ofSeconds(60);

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> tickets = new ConcurrentHashMap<>();

    private record Entry(AuthorizationResponse user, Instant expiresAt) { }

    @Autowired
    public WebSocketTicketService() {
        this(Clock.systemUTC());
    }

    WebSocketTicketService(Clock clock) {
        this.clock = clock;
    }

    public String issue(AuthorizationResponse user) {
        Instant now = clock.instant();
        tickets.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(ticket, new Entry(user, now.plus(TTL)));
        return ticket;
    }

    /** Devuelve el usuario del ticket y lo invalida; vacío si no existe, ya se usó o venció. */
    public Optional<AuthorizationResponse> redeem(String ticket) {
        if (ticket == null || ticket.isBlank()) return Optional.empty();
        Entry entry = tickets.remove(ticket);
        if (entry == null || !entry.expiresAt().isAfter(clock.instant())) return Optional.empty();
        return Optional.of(entry.user());
    }
}
