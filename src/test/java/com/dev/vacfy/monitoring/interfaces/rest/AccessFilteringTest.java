package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.exceptions.ForbiddenException;
import com.dev.vacfy.monitoring.domain.model.queries.GetAlertsQuery;
import com.dev.vacfy.monitoring.domain.model.queries.GetReadingsQuery;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AccessScope;
import com.dev.vacfy.monitoring.domain.model.valueobjects.ContainerStatus;
import com.dev.vacfy.monitoring.domain.model.valueobjects.ContainerSummary;
import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.domain.services.ContainerAccessService;
import com.dev.vacfy.monitoring.domain.services.DashboardQueryService;
import com.dev.vacfy.monitoring.domain.services.MonitoringCommandService;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/** Una enfermera solo ve sus termos en lecturas, dashboard y alertas; el supervisor ve todo. */
class AccessFilteringTest {
    private static final Instant DESDE = Instant.parse("2026-10-07T07:00:00Z");
    private final ContainerAccessService access = mock(ContainerAccessService.class);
    private final MonitoringQueryService monitoringQueries = mock(MonitoringQueryService.class);
    private final DashboardQueryService dashboard = mock(DashboardQueryService.class);

    private static MockHttpServletRequest request(String userId, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("userRole", role);
        return request;
    }

    private void nurseHas001() {
        when(access.scope(new Viewer("ana", false))).thenReturn(new AccessScope(false, Map.of("001", DESDE)));
    }

    private void supervisor() {
        when(access.scope(new Viewer("sup", true))).thenReturn(AccessScope.everything());
    }

    private static ContainerSummary summary(String contenedor) {
        return new ContainerSummary(contenedor, ContainerStatus.OK, 5.0, 50.0, Instant.now(),
                new TemperatureLimits("PAI", 2, 8, true), 0, 0, null, 0, null);
    }

    @Test
    void nurseCannotReadReadingsOfAnotherContainer() {
        nurseHas001();
        var controller = new ReadingsController(monitoringQueries, access);
        assertThrows(ForbiddenException.class, () -> controller.getReadings("002", null, null, request("ana", "ENFERMERA")));
        verifyNoInteractions(monitoringQueries);
    }

    @Test
    void nurseReadingsStartWhenTheContainerWasAssigned() {
        nurseHas001();
        when(monitoringQueries.handle(any(GetReadingsQuery.class))).thenReturn(List.of());
        new ReadingsController(monitoringQueries, access).getReadings("001", null, null, request("ana", "ENFERMERA"));
        ArgumentCaptor<GetReadingsQuery> query = ArgumentCaptor.forClass(GetReadingsQuery.class);
        verify(monitoringQueries).handle(query.capture());
        assertEquals(DESDE, query.getValue().notBefore());
    }

    @Test
    void supervisorReadsTheWholeHistory() {
        supervisor();
        when(monitoringQueries.handle(any(GetReadingsQuery.class))).thenReturn(List.of());
        new ReadingsController(monitoringQueries, access).getReadings("002", null, null, request("sup", "SUPERVISOR"));
        ArgumentCaptor<GetReadingsQuery> query = ArgumentCaptor.forClass(GetReadingsQuery.class);
        verify(monitoringQueries).handle(query.capture());
        assertNull(query.getValue().notBefore());
    }

    @Test
    void nurseDashboardOnlyHasHerContainers() {
        nurseHas001();
        when(dashboard.getSummary(Set.of("001"))).thenReturn(List.of(summary("001")));
        var body = new DashboardController(dashboard, access).getSummary(request("ana", "ENFERMERA")).getBody();
        assertEquals(List.of("001"), body.containers().stream().map(c -> c.contenedor()).toList());
        verify(dashboard, never()).getSummary();
    }

    @Test
    void supervisorDashboardHasEveryContainer() {
        supervisor();
        when(dashboard.getSummary()).thenReturn(List.of(summary("001"), summary("002")));
        var body = new DashboardController(dashboard, access).getSummary(request("sup", "SUPERVISOR")).getBody();
        assertEquals(2, body.containers().size());
    }

    @Test
    void nurseAlertsAreLimitedToHerContainers() {
        nurseHas001();
        var controller = new AlertsController(monitoringQueries, mock(MonitoringCommandService.class), access);
        assertThrows(ForbiddenException.class, () -> controller.getAlerts(null, "002", request("ana", "ENFERMERA")));

        when(monitoringQueries.handle(any(GetAlertsQuery.class))).thenReturn(List.of());
        controller.getAlerts(null, null, request("ana", "ENFERMERA"));
        ArgumentCaptor<GetAlertsQuery> query = ArgumentCaptor.forClass(GetAlertsQuery.class);
        verify(monitoringQueries).handle(query.capture());
        assertEquals(Set.of("001"), Set.copyOf(query.getValue().containers()));
    }

    @Test
    void supervisorAlertsAreNotFiltered() {
        supervisor();
        when(monitoringQueries.handle(any(GetAlertsQuery.class))).thenReturn(List.of());
        new AlertsController(monitoringQueries, mock(MonitoringCommandService.class), access)
                .getAlerts(null, null, request("sup", "SUPERVISOR"));
        ArgumentCaptor<GetAlertsQuery> query = ArgumentCaptor.forClass(GetAlertsQuery.class);
        verify(monitoringQueries).handle(query.capture());
        assertNull(query.getValue().containers());
    }
}
