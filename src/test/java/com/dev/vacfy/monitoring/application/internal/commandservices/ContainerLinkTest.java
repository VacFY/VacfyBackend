package com.dev.vacfy.monitoring.application.internal.commandservices;

import com.dev.vacfy.monitoring.application.internal.outboundservices.AssignmentEventPublisher;
import com.dev.vacfy.monitoring.domain.exceptions.ForbiddenException;
import com.dev.vacfy.monitoring.domain.exceptions.InvalidDataException;
import com.dev.vacfy.monitoring.domain.exceptions.TooManyRequestsException;
import com.dev.vacfy.monitoring.domain.model.aggregates.Container;
import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;
import com.dev.vacfy.monitoring.domain.model.commands.LinkContainerCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterContainerCommand;
import com.dev.vacfy.monitoring.domain.model.commands.UnlinkContainerCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AssignmentEndReason;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.domain.services.ContainerAccessService;
import com.dev.vacfy.monitoring.domain.services.ContainerKeys;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerAssignmentRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Vincular y desvincular termos, con repositorios en memoria. */
class ContainerLinkTest {
    private static final Viewer SUPERVISOR = new Viewer("sup-1", true);
    private static final Viewer ANA = new Viewer("ana", false);
    private static final Viewer BETO = new Viewer("beto", false);

    private final Map<String, Container> containers = new HashMap<>();
    private final List<ContainerAssignment> assignments = new ArrayList<>();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-07T12:00:00Z"));
    private final ContainerAccessService access = mock(ContainerAccessService.class);
    private final AssignmentEventPublisher events = mock(AssignmentEventPublisher.class);
    private ContainerCommandServiceImpl service;
    private String key;

    @BeforeEach
    void setUp() {
        ContainerRepository containerRepository = mock(ContainerRepository.class);
        when(containerRepository.existsById(anyString())).thenAnswer(i -> containers.containsKey(i.<String>getArgument(0)));
        when(containerRepository.findById(anyString())).thenAnswer(i -> Optional.ofNullable(containers.get(i.<String>getArgument(0))));
        when(containerRepository.save(any(Container.class))).thenAnswer(i -> {
            Container c = i.getArgument(0);
            containers.put(c.getCodigo(), c);
            return c;
        });

        ContainerAssignmentRepository assignmentRepository = mock(ContainerAssignmentRepository.class);
        when(assignmentRepository.findByContenedorAndHastaIsNull(anyString())).thenAnswer(i -> assignments.stream()
                .filter(a -> a.isActive() && a.getContenedor().equals(i.getArgument(0))).findFirst());
        when(assignmentRepository.saveAndFlush(any(ContainerAssignment.class))).thenAnswer(i -> store(i.getArgument(0)));
        when(assignmentRepository.save(any(ContainerAssignment.class))).thenAnswer(i -> store(i.getArgument(0)));

        LinkAttemptLimiter limiter = new LinkAttemptLimiter(5, Duration.ofMinutes(10), clock);
        service = new ContainerCommandServiceImpl(containerRepository, assignmentRepository, access, limiter, events, clock);
        key = service.handle(new RegisterContainerCommand(SUPERVISOR, "001", "Termo Posta Huambos")).clave();
    }

    private ContainerAssignment store(ContainerAssignment assignment) {
        if (!assignments.contains(assignment)) assignments.add(assignment);
        return assignment;
    }

    private List<ContainerAssignment> active() {
        return assignments.stream().filter(ContainerAssignment::isActive).toList();
    }

    @Test
    void onlyTheSupervisorRegistersContainers() {
        assertThrows(ForbiddenException.class, () -> service.handle(new RegisterContainerCommand(ANA, "002", null)));
        assertTrue(key.matches("[A-Z2-9]{3}-[A-Z2-9]{3}"), key);
        assertNotEquals(ContainerKeys.normalize(key), containers.get("001").getClaveHash(), "solo se guarda el hash");
    }

    @Test
    void correctKeyLinksTheContainer() {
        var result = service.handle(new LinkContainerCommand(ANA, "001", key.toLowerCase().replace("-", " ")));
        assertTrue(result.created());
        assertEquals("ana", result.assignment().getUserId());
        assertEquals(1, active().size());
        verify(access).invalidate("ana");
        verify(events).assignmentChanged(eq("001"), argThat(users -> users.contains("ana")));
    }

    @Test
    void wrongKeyOrUnknownCodeGiveTheSameMessage() {
        var wrongKey = assertThrows(InvalidDataException.class, () -> service.handle(new LinkContainerCommand(ANA, "001", "AAA-AAA")));
        var unknown = assertThrows(InvalidDataException.class, () -> service.handle(new LinkContainerCommand(ANA, "999", key)));
        assertEquals("Código o clave incorrectos.", wrongKey.getMessage());
        assertEquals(wrongKey.getMessage(), unknown.getMessage());
        assertTrue(active().isEmpty());
    }

    @Test
    void fiveFailuresInTenMinutesBlockTheUser() {
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidDataException.class, () -> service.handle(new LinkContainerCommand(ANA, "001", "AAA-AAA")));
        }
        assertThrows(TooManyRequestsException.class, () -> service.handle(new LinkContainerCommand(ANA, "001", key)),
                "bloqueado aunque ahora la clave sea correcta");
        clock.advance(Duration.ofMinutes(11));
        assertTrue(service.handle(new LinkContainerCommand(ANA, "001", key)).created());
    }

    @Test
    void failuresAgainstOneContainerBlockItForEveryone() {
        for (int i = 0; i < 5; i++) {
            Viewer attacker = new Viewer("atacante-" + i, false);
            assertThrows(InvalidDataException.class, () -> service.handle(new LinkContainerCommand(attacker, "001", "AAA-AAA")));
        }
        assertThrows(TooManyRequestsException.class, () -> service.handle(new LinkContainerCommand(BETO, "001", key)));
    }

    @Test
    void shiftChangeClosesThePreviousAssignment() {
        service.handle(new LinkContainerCommand(ANA, "001", key));
        clock.advance(Duration.ofHours(12));
        service.handle(new LinkContainerCommand(BETO, "001", key));

        ContainerAssignment anas = assignments.stream().filter(a -> a.getUserId().equals("ana")).findFirst().orElseThrow();
        assertFalse(anas.isActive());
        assertEquals(AssignmentEndReason.TOMADO_POR_OTRA, anas.getMotivoCierre());
        assertEquals(List.of("beto"), active().stream().map(ContainerAssignment::getUserId).toList());
        verify(access, times(2)).invalidate("ana"); // al vincularse y al perder el termo
        verify(events).assignmentChanged(eq("001"), argThat(users -> users.containsAll(Set.of("ana", "beto"))));
    }

    @Test
    void linkingAgainDoesNotDuplicate() {
        service.handle(new LinkContainerCommand(ANA, "001", key));
        var again = service.handle(new LinkContainerCommand(ANA, "001", key));
        assertFalse(again.created());
        assertEquals(1, assignments.size());
    }

    @Test
    void unlinkingSomeoneElsesContainerIsForbidden() {
        service.handle(new LinkContainerCommand(ANA, "001", key));
        assertThrows(ForbiddenException.class, () -> service.handle(new UnlinkContainerCommand(BETO, "001")));
        assertEquals(1, active().size());
    }

    @Test
    void ownerHandsOverAndSupervisorCanUnlinkAnyone() {
        service.handle(new LinkContainerCommand(ANA, "001", key));
        assertEquals(AssignmentEndReason.ENTREGADO, service.handle(new UnlinkContainerCommand(ANA, "001")).getMotivoCierre());

        service.handle(new LinkContainerCommand(BETO, "001", key));
        assertEquals(AssignmentEndReason.DESVINCULADO_POR_SUPERVISOR,
                service.handle(new UnlinkContainerCommand(SUPERVISOR, "001")).getMotivoCierre());
        assertTrue(active().isEmpty());
    }

    @Test
    void temporaryCodeWorksOnceAndExpires() {
        Container container = containers.get("001");
        container.issueTemporaryCode(ContainerCommandServiceImpl.KEY_ENCODER.encode("M4R8XZ"), clock.instant().plus(Duration.ofMinutes(5)));
        assertTrue(service.handle(new LinkContainerCommand(ANA, "001", "m4r8xz")).created());
        assertFalse(container.hasTemporaryCode(clock.instant()), "se invalida al usarse");
        assertThrows(InvalidDataException.class, () -> service.handle(new LinkContainerCommand(BETO, "001", "M4R8XZ")));

        container.issueTemporaryCode(ContainerCommandServiceImpl.KEY_ENCODER.encode("Q2W3E4"), clock.instant().plus(Duration.ofMinutes(5)));
        clock.advance(Duration.ofMinutes(6));
        assertThrows(InvalidDataException.class, () -> service.handle(new LinkContainerCommand(BETO, "001", "Q2W3E4")), "venció");
    }

    @Test
    void regeneratedKeyReplacesTheOldOne() {
        String newKey = service.handle(new com.dev.vacfy.monitoring.domain.model.commands.RegenerateContainerKeyCommand(SUPERVISOR, "001")).clave();
        assertThrows(InvalidDataException.class, () -> service.handle(new LinkContainerCommand(ANA, "001", key)));
        assertTrue(service.handle(new LinkContainerCommand(ANA, "001", newKey)).created());
    }

    /** Reloj que los tests pueden adelantar. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
