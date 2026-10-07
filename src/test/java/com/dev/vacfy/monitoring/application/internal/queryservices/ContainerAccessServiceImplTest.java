package com.dev.vacfy.monitoring.application.internal.queryservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AccessScope;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerAssignmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContainerAccessServiceImplTest {
    private static final Instant DESDE = Instant.parse("2026-10-07T07:00:00Z");
    private final ContainerAssignmentRepository repository = mock(ContainerAssignmentRepository.class);
    private final ContainerAccessServiceImpl access =
            new ContainerAccessServiceImpl(repository, Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void supervisorSeesEveryContainer() {
        AccessScope scope = access.scope(new Viewer("sup", true));
        assertTrue(scope.all());
        assertTrue(scope.canSee("001"));
        assertTrue(scope.canSee("cualquiera"));
        assertNull(scope.since("001"), "sin límite de historial");
        verifyNoInteractions(repository);
    }

    @Test
    void nurseSeesOnlyHerActiveAssignments() {
        when(repository.findByUserIdAndHastaIsNull("ana")).thenReturn(List.of(new ContainerAssignment("001", "ana", DESDE)));
        Viewer ana = new Viewer("ana", false);
        assertTrue(access.canSee(ana, "001"));
        assertFalse(access.canSee(ana, "002"));
        assertEquals(DESDE, access.scope(ana).since("001"));
        assertEquals(List.of("001"), access.visibleContainers(ana));
    }

    @Test
    void withoutSessionNothingIsVisible() {
        assertFalse(access.canSee(new Viewer(null, false), "001"));
        assertFalse(access.canSee(null, "001"));
    }

    @Test
    void assignmentsAreCachedUntilInvalidated() {
        when(repository.findByUserIdAndHastaIsNull("ana")).thenReturn(List.of());
        Viewer ana = new Viewer("ana", false);
        assertFalse(access.canSee(ana, "001"));
        assertFalse(access.canSee(ana, "001"));
        verify(repository, times(1)).findByUserIdAndHastaIsNull("ana");

        when(repository.findByUserIdAndHastaIsNull("ana")).thenReturn(List.of(new ContainerAssignment("001", "ana", DESDE)));
        access.invalidate("ana");
        assertTrue(access.canSee(ana, "001"), "tras vincular se ve al momento");
    }
}
