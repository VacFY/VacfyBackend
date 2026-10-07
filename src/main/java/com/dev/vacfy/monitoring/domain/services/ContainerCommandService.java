package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerAssignment;
import com.dev.vacfy.monitoring.domain.model.commands.LinkContainerCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegenerateContainerKeyCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterContainerCommand;
import com.dev.vacfy.monitoring.domain.model.commands.UnlinkContainerCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.IssuedKey;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LinkResult;

public interface ContainerCommandService {
    /** Solo SUPERVISOR. Devuelve la clave en claro una única vez. */
    IssuedKey handle(RegisterContainerCommand command);

    /** Solo SUPERVISOR. Invalida la clave anterior. */
    IssuedKey handle(RegenerateContainerKeyCommand command);

    /**
     * Vincula el termo a quien envía el código y la clave (fija o temporal). Si lo tenía otra persona, cierra esa
     * asignación (TOMADO_POR_OTRA): es el cambio de turno.
     */
    LinkResult handle(LinkContainerCommand command);

    /** Entrega el termo (ENTREGADO), o lo desvincula el supervisor (DESVINCULADO_POR_SUPERVISOR). */
    ContainerAssignment handle(UnlinkContainerCommand command);
}
