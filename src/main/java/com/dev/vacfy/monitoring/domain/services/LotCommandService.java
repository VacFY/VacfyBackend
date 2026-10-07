package com.dev.vacfy.monitoring.domain.services;

import com.dev.vacfy.monitoring.domain.model.commands.CloseLotCommand;
import com.dev.vacfy.monitoring.domain.model.commands.RegisterLotCommand;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotView;

public interface LotCommandService {
    LotView handle(RegisterLotCommand command);

    LotView handle(CloseLotCommand command);
}
