package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.model.commands.CreateVaccineCommand;
import com.dev.vacfy.monitoring.domain.model.commands.UpdateVaccineCommand;

public interface VaccineCommandService {
    VaccineProfile handle(CreateVaccineCommand command);

    VaccineProfile handle(UpdateVaccineCommand command);
}
