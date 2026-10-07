package com.dev.vacfy.user.domain.services;

import com.dev.vacfy.user.domain.model.commands.CreateDeviceCommand;
import com.dev.vacfy.user.domain.model.commands.DeleteDeviceCommand;
import com.dev.vacfy.user.domain.model.commands.UpdateDeviceCommand;

public interface DeviceCommandService {
    void handle(CreateDeviceCommand createDeviceCommand);
    void handle(UpdateDeviceCommand updateDeviceCommand);
    void handle(DeleteDeviceCommand deleteDeviceCommand);
}
