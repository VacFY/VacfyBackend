package com.dev.vacfy.monitoring.interfaces.websocket;

import com.dev.vacfy.iot.interfaces.websocket.IotWebSocketHandler;
import com.dev.vacfy.monitoring.domain.model.queries.GetAlertsQuery;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AccessScope;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import com.dev.vacfy.monitoring.domain.services.ContainerAccessService;
import com.dev.vacfy.monitoring.domain.services.MonitoringQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** /ws/device y /ws/alerts envían cada mensaje solo a quien puede ver ese termo. */
class WebSocketFilteringTest {
    private final ContainerAccessService access = mock(ContainerAccessService.class);
    private final MonitoringQueryService queries = mock(MonitoringQueryService.class);
    private WebSocketSession ana;
    private WebSocketSession beto;
    private WebSocketSession supervisor;

    private static WebSocketSession session(String userId, String role) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>(Map.of("userId", userId, "userRole", role));
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn(userId);
        return session;
    }

    @BeforeEach
    void setUp() {
        ana = session("ana", "ENFERMERA");
        beto = session("beto", "ENFERMERA");
        supervisor = session("sup", "SUPERVISOR");
        Map<Viewer, AccessScope> scopes = Map.of(
                new Viewer("ana", false), new AccessScope(false, Map.of("001", Instant.EPOCH)),
                new Viewer("beto", false), new AccessScope(false, Map.of("002", Instant.EPOCH)),
                new Viewer("sup", true), AccessScope.everything());
        when(access.scope(any())).thenAnswer(i -> scopes.getOrDefault(i.<Viewer>getArgument(0), AccessScope.nothing()));
        when(access.canSee(any(), any())).thenAnswer(i -> scopes.getOrDefault(i.<Viewer>getArgument(0), AccessScope.nothing())
                .canSee(i.getArgument(1)));
    }

    @Test
    void telemetryGoesOnlyToWhoCanSeeTheContainer() throws Exception {
        IotWebSocketHandler handler = new IotWebSocketHandler(access);
        for (var s : List.of(ana, beto, supervisor)) handler.afterConnectionEstablished(s);

        handler.sendToContainer("001", "{\"contenedor\":\"001\",\"temperatura\":5.0}");

        verify(ana).sendMessage(any(TextMessage.class));
        verify(supervisor).sendMessage(any(TextMessage.class));
        verify(beto, never()).sendMessage(any());
    }

    @Test
    void alertsGoOnlyToWhoCanSeeTheContainer() throws Exception {
        when(queries.handle(any(GetAlertsQuery.class))).thenReturn(List.of());
        AlertWebSocketHandler handler = new AlertWebSocketHandler(queries, access, new ObjectMapper());
        for (var s : List.of(ana, beto, supervisor)) handler.afterConnectionEstablished(s);

        handler.sendAlert("002", "{\"id\":1,\"contenedor\":\"002\"}");

        verify(beto).sendMessage(any(TextMessage.class));
        verify(supervisor).sendMessage(any(TextMessage.class));
        verify(ana, never()).sendMessage(any());
    }

    @Test
    void openAlertsOnConnectAreLimitedToTheNursesContainers() throws Exception {
        when(queries.handle(any(GetAlertsQuery.class))).thenReturn(List.of());
        new AlertWebSocketHandler(queries, access, new ObjectMapper()).afterConnectionEstablished(ana);
        ArgumentCaptor<GetAlertsQuery> query = ArgumentCaptor.forClass(GetAlertsQuery.class);
        verify(queries).handle(query.capture());
        assertEquals(Set.of("001"), Set.copyOf(query.getValue().containers()));
    }

    @Test
    void assignmentEventGoesToAffectedUsersAndSupervisors() throws Exception {
        when(queries.handle(any(GetAlertsQuery.class))).thenReturn(List.of());
        AlertWebSocketHandler handler = new AlertWebSocketHandler(queries, access, new ObjectMapper());
        for (var s : List.of(ana, beto, supervisor)) handler.afterConnectionEstablished(s);

        new WebSocketAssignmentEventPublisher(handler).assignmentChanged("001", Set.of("ana"));

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
        verify(ana).sendMessage(sent.capture());
        assertEquals("{\"tipo\":\"ASIGNACION_CAMBIADA\",\"contenedor\":\"001\"}", sent.getValue().getPayload());
        verify(supervisor).sendMessage(any(TextMessage.class));
        verify(beto, never()).sendMessage(any());
    }
}
