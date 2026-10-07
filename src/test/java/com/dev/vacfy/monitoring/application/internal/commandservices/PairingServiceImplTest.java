package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.Container;
import com.dev.vacfy.monitoring.domain.model.valueobjects.PairingCode;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PairingServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-10-07T18:00:00Z");
    private final ContainerRepository repository = mock(ContainerRepository.class);
    private final PairingServiceImpl pairing =
            new PairingServiceImpl(repository, Duration.ofMinutes(5), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void registeredContainerGetsAFiveMinuteCode() {
        Container container = new Container("001", "Termo", "hash", NOW);
        when(repository.findById("001")).thenReturn(Optional.of(container));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        PairingCode code = pairing.issueTemporaryCode("001").orElseThrow();

        assertTrue(code.codigo().matches("[A-Z2-9]{6}"), code.codigo());
        assertEquals(NOW.plus(Duration.ofMinutes(5)), code.venceEn());
        assertEquals("vacty:001:" + code.codigo(), code.qr());
        assertTrue(container.hasTemporaryCode(NOW));
        assertTrue(ContainerCommandServiceImpl.KEY_ENCODER.matches(code.codigo(), container.getCodigoTemporalHash()),
                "se guarda el hash, no el código");
        assertFalse(container.hasTemporaryCode(NOW.plus(Duration.ofMinutes(5))), "vence a los 5 minutos");
    }

    @Test
    void aNewCodeReplacesThePreviousOne() {
        Container container = new Container("001", "Termo", "hash", NOW);
        when(repository.findById("001")).thenReturn(Optional.of(container));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        PairingCode first = pairing.issueTemporaryCode("001").orElseThrow();
        PairingCode second = pairing.issueTemporaryCode("001").orElseThrow();

        assertTrue(ContainerCommandServiceImpl.KEY_ENCODER.matches(second.codigo(), container.getCodigoTemporalHash()));
        if (!first.codigo().equals(second.codigo())) {
            assertFalse(ContainerCommandServiceImpl.KEY_ENCODER.matches(first.codigo(), container.getCodigoTemporalHash()));
        }
    }

    @Test
    void unregisteredContainerGetsNoCode() {
        when(repository.findById("999")).thenReturn(Optional.empty());
        assertTrue(pairing.issueTemporaryCode("999").isEmpty());
        verify(repository, never()).save(any());
    }
}
