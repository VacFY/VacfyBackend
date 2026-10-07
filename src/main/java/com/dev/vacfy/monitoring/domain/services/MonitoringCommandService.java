package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerProfile;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import com.dev.vacfy.monitoring.domain.model.commands.AssignContainerProfileCommand;
import com.dev.vacfy.monitoring.domain.model.commands.CreateVaccineProfileCommand;
import com.dev.vacfy.monitoring.domain.model.commands.ProcessTelemetryCommand;

import java.util.Optional;

public interface MonitoringCommandService {
    /** Guarda la lectura (muestreada) y evalúa las reglas. */
    void handle(ProcessTelemetryCommand command);

    Optional<Alert> handle(AcknowledgeAlertCommand command);

    VaccineProfile handle(CreateVaccineProfileCommand command);

    ContainerProfile handle(AssignContainerProfileCommand command);

    /** Revisa qué contenedores dejaron de enviar datos. */
    void checkOfflineContainers();
}
