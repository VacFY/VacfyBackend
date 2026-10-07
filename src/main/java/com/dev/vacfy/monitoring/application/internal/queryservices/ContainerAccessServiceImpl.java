package com.dev.vacfy.monitoring.application.internal.queryservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AccessScope;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.domain.services.ContainerAccessService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Asignaciones activas de cada enfermera, en caché por 30 s. Así los WebSockets pueden filtrar cada lectura
 * sin consultar la base de datos; vincular o desvincular invalida el caché de los usuarios afectados.
 */
@Service
public class ContainerAccessServiceImpl implements ContainerAccessService {
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private record Cached(Map<String, Instant> assignedSince, Instant loadedAt) { }

    private final ContainerAssignmentRepository assignmentRepository;
    private final Clock clock;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Autowired
    public ContainerAccessServiceImpl(ContainerAssignmentRepository assignmentRepository) {
        this(assignmentRepository, Clock.systemUTC());
    }

    ContainerAccessServiceImpl(ContainerAssignmentRepository assignmentRepository, Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.clock = clock;
    }

    @Override
    public AccessScope scope(Viewer viewer) {
        if (viewer == null || viewer.userId() == null) return AccessScope.nothing();
        if (viewer.supervisor()) return AccessScope.everything();
        return new AccessScope(false, assignedSince(viewer.userId()));
    }

    @Override
    public boolean canSee(Viewer viewer, String contenedor) {
        return scope(viewer).canSee(contenedor);
    }

    @Override
    public List<String> visibleContainers(Viewer viewer) {
        if (viewer == null || viewer.userId() == null) return List.of();
        return assignedSince(viewer.userId()).keySet().stream().sorted().toList();
    }

    @Override
    public void invalidate(String userId) {
        if (userId != null) cache.remove(userId);
    }

    private Map<String, Instant> assignedSince(String userId) {
        Instant now = clock.instant();
        Cached cached = cache.get(userId);
        if (cached != null && cached.loadedAt().plus(CACHE_TTL).isAfter(now)) return cached.assignedSince();
        Map<String, Instant> loaded = assignmentRepository.findByUserIdAndHastaIsNull(userId).stream()
                .collect(Collectors.toUnmodifiableMap(ContainerAssignment::getContenedor, ContainerAssignment::getDesde));
        cache.put(userId, new Cached(loaded, now));
        return loaded;
    }
}
