package com.dev.vacfy.monitoring.application.internal.queryservices;

import com.dev.vacfy.monitoring.domain.exceptions.ForbiddenException;
import com.dev.vacfy.monitoring.domain.model.aggregates.Container;
import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;
import com.dev.vacfy.monitoring.domain.model.valueobjects.*;
import com.dev.vacfy.monitoring.domain.services.ContainerQueryService;
import com.dev.vacfy.monitoring.domain.services.DashboardQueryService;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerAssignmentRepository;
import com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories.ContainerRepository;
import com.dev.vacfy.user.interfaces.acl.ProfileContactData;
import com.dev.vacfy.user.interfaces.acl.ProfileContextFacade;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContainerQueryServiceImpl implements ContainerQueryService {
    private final ContainerRepository containerRepository;
    private final ContainerAssignmentRepository assignmentRepository;
    private final DashboardQueryService dashboardQueryService;
    private final ProfileContextFacade profileContextFacade;

    public ContainerQueryServiceImpl(ContainerRepository containerRepository,
                                     ContainerAssignmentRepository assignmentRepository,
                                     DashboardQueryService dashboardQueryService,
                                     ProfileContextFacade profileContextFacade) {
        this.containerRepository = containerRepository;
        this.assignmentRepository = assignmentRepository;
        this.dashboardQueryService = dashboardQueryService;
        this.profileContextFacade = profileContextFacade;
    }

    @Override
    public List<ContainerOverview> getAllContainers(Viewer viewer) {
        if (viewer == null || !viewer.supervisor()) throw new ForbiddenException("Solo el supervisor puede ver todos los termos.");
        Map<String, Container> containers = containerRepository.findAll().stream()
                .collect(Collectors.toMap(Container::getCodigo, Function.identity()));
        Map<String, ContainerAssignment> active = assignmentRepository.findByHastaIsNull().stream()
                .collect(Collectors.toMap(ContainerAssignment::getContenedor, Function.identity(), (a, b) -> a));
        Map<String, ProfileContactData> contacts = contacts(active.values());
        return dashboardQueryService.getSummary().stream()
                .map(summary -> {
                    ContainerAssignment assignment = active.get(summary.contenedor());
                    return new ContainerOverview(summary.contenedor(), containers.get(summary.contenedor()), summary,
                            assignment == null ? null : view(assignment, contacts));
                })
                .toList();
    }

    @Override
    public List<MyContainer> getMyContainers(Viewer viewer) {
        if (viewer == null || viewer.userId() == null) return List.of();
        Map<String, ContainerAssignment> mine = assignmentRepository.findByUserIdAndHastaIsNull(viewer.userId()).stream()
                .collect(Collectors.toMap(ContainerAssignment::getContenedor, Function.identity(), (a, b) -> a));
        if (mine.isEmpty()) return List.of();
        Map<String, Container> containers = containerRepository.findAllById(mine.keySet()).stream()
                .collect(Collectors.toMap(Container::getCodigo, Function.identity()));
        return dashboardQueryService.getSummary(mine.keySet()).stream()
                .map(summary -> new MyContainer(containers.get(summary.contenedor()), summary,
                        mine.get(summary.contenedor()).getDesde()))
                .toList();
    }

    @Override
    public List<AssignmentView> getAssignments(Viewer viewer, String codigo) {
        String contenedor = codigo == null ? "" : codigo.trim();
        boolean holder = viewer != null && assignmentRepository.findByContenedorAndHastaIsNull(contenedor)
                .map(current -> current.getUserId().equals(viewer.userId()))
                .orElse(false);
        if (viewer == null || !(viewer.supervisor() || holder)) {
            throw new ForbiddenException("Solo el supervisor o quien tiene el termo ahora puede ver su historial.");
        }
        List<ContainerAssignment> history = assignmentRepository.findByContenedorOrderByDesdeDesc(contenedor);
        Map<String, ProfileContactData> contacts = contacts(history);
        return history.stream().map(assignment -> view(assignment, contacts)).toList();
    }

    private Map<String, ProfileContactData> contacts(Collection<ContainerAssignment> assignments) {
        Set<String> userIds = assignments.stream().map(ContainerAssignment::getUserId).collect(Collectors.toSet());
        return userIds.isEmpty() ? Map.of() : profileContextFacade.getContactData(userIds);
    }

    private static AssignmentView view(ContainerAssignment assignment, Map<String, ProfileContactData> contacts) {
        ProfileContactData contact = contacts.get(assignment.getUserId());
        return new AssignmentView(assignment, contact == null ? null
                : new Assignee(contact.userId(), contact.dni(), contact.fullName()));
    }
}
