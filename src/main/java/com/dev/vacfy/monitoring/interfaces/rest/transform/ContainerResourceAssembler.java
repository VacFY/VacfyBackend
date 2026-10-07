package com.dev.vacfy.monitoring.interfaces.rest.transform;

import com.dev.vacfy.monitoring.domain.model.valueobjects.*;
import com.dev.vacfy.monitoring.interfaces.rest.resources.*;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ContainerOverviewResource.AssignedToResource;

import java.time.Instant;

public final class ContainerResourceAssembler {
    private ContainerResourceAssembler() { }

    public static ContainerKeyResource toResource(IssuedKey issued) {
        var container = issued.container();
        return new ContainerKeyResource(container.getCodigo(), container.getNombre(), issued.clave(), container.isActivo(),
                container.getCreadoEn().toString());
    }

    public static LinkResultResource toResource(LinkResult result) {
        return new LinkResultResource(result.assignment().getContenedor(), result.container().getNombre(),
                iso(result.assignment().getDesde()), result.created());
    }

    public static ContainerAssignmentResource toResource(AssignmentView view) {
        var assignment = view.assignment();
        Assignee assignee = view.assignee();
        return new ContainerAssignmentResource(assignment.getId(), assignment.getContenedor(), assignment.getUserId(),
                assignee == null ? null : new AssigneeResource(assignee.userId(), assignee.dni(), assignee.fullName()),
                iso(assignment.getDesde()), iso(assignment.getHasta()),
                assignment.getMotivoCierre() == null ? null : assignment.getMotivoCierre().name());
    }

    public static MyContainerResource toResource(MyContainer mine) {
        ContainerSummary s = mine.summary();
        return new MyContainerResource(s.contenedor(), mine.container() == null ? null : mine.container().getNombre(),
                iso(mine.asignadoDesde()), s.status().name(), s.temperatura(), s.humedad(), iso(s.lastReadingAt()),
                DashboardResourceAssembler.toResource(s.limits()), s.activeLots(), s.expiredLots(),
                DashboardResourceAssembler.toResource(s.nextExpiry()), s.openAlerts(),
                s.highestSeverity() == null ? null : s.highestSeverity().name());
    }

    public static ContainerOverviewResource toResource(ContainerOverview overview) {
        ContainerSummary s = overview.summary();
        AssignedToResource assignedTo = null;
        if (overview.assignment() != null) {
            var assignment = overview.assignment().assignment();
            Assignee assignee = overview.assignment().assignee();
            assignedTo = new AssignedToResource(assignment.getUserId(), assignee == null ? null : assignee.dni(),
                    assignee == null ? null : assignee.fullName(), iso(assignment.getDesde()));
        }
        return new ContainerOverviewResource(overview.contenedor(),
                overview.container() == null ? null : overview.container().getNombre(), overview.registered(),
                overview.container() != null && overview.container().isActivo(), assignedTo, s.status().name(),
                s.temperatura(), s.humedad(), iso(s.lastReadingAt()), DashboardResourceAssembler.toResource(s.limits()),
                s.activeLots(), s.expiredLots(), DashboardResourceAssembler.toResource(s.nextExpiry()), s.openAlerts(),
                s.highestSeverity() == null ? null : s.highestSeverity().name());
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
