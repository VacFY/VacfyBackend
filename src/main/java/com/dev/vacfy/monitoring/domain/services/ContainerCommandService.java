package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.commands.RegenerateContainerKeyCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterContainerCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.IssuedKey;

public interface ContainerCommandService {
    /** Solo SUPERVISOR. Devuelve la clave en claro una única vez. */
    IssuedKey handle(RegisterContainerCommand command);

    /** Solo SUPERVISOR. Invalida la clave anterior. */
    IssuedKey handle(RegenerateContainerKeyCommand command);
}
