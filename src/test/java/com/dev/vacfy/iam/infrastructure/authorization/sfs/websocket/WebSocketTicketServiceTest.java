package com.dev.vacfy.iam.infrastructure.authorization.sfs.websocket;

import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.AuthorizationResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketTicketServiceTest {
    private static final AuthorizationResponse ANA = new AuthorizationResponse("ana", "ENFERMERA");

    private static class MovableClock extends Clock {
        Instant now = Instant.parse("2026-10-07T12:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void ticketWorksOnlyOnce() {
        var service = new WebSocketTicketService();
        String ticket = service.issue(ANA);
        assertEquals(ANA, service.redeem(ticket).orElseThrow());
        assertTrue(service.redeem(ticket).isEmpty());
    }

    @Test
    void unknownOrEmptyTicketIsRejected() {
        var service = new WebSocketTicketService();
        assertTrue(service.redeem("no-existe").isEmpty());
        assertTrue(service.redeem("").isEmpty());
        assertTrue(service.redeem(null).isEmpty());
    }

    @Test
    void ticketExpiresAfterSixtySeconds() {
        var clock = new MovableClock();
        var service = new WebSocketTicketService(clock);
        String used = service.issue(ANA);
        String late = service.issue(ANA);
        clock.now = clock.now.plusSeconds(59);
        assertTrue(service.redeem(used).isPresent());
        clock.now = clock.now.plusSeconds(1);
        assertTrue(service.redeem(late).isEmpty());
    }

    @Test
    void eachTicketIsDifferent() {
        var service = new WebSocketTicketService();
        assertNotEquals(service.issue(ANA), service.issue(ANA));
    }
}
