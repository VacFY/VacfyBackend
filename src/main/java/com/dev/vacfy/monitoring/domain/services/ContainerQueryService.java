package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AssignmentView;
import com.dev.vacfy.monitoring.domain.model.valueobjects.ContainerOverview;
import com.dev.vacfy.monitoring.domain.model.valueobjects.MyContainer;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;

import java.util.List;

public interface ContainerQueryService {
    /** Solo SUPERVISOR: todos los termos (registrados y no registrados) con quién los tiene. */
    List<ContainerOverview> getAllContainers(Viewer viewer);

    /** Termos que el usuario tiene asignados ahora. */
    List<MyContainer> getMyContainers(Viewer viewer);

    /** Quién tuvo el termo, del más reciente al más antiguo. SUPERVISOR, o quien lo tiene ahora. */
    List<AssignmentView> getAssignments(Viewer viewer, String codigo);
}
