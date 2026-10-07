package com.dev.vacfy.user.interfaces.rest.transform;

import com.dev.vacfy.user.domain.model.commands.UpdateDeviceCommand;
import com.dev.vacfy.user.interfaces.rest.resources.UpdateDeviceResource;

public class UpdateDeviceCommandFromResourceAssembler {
    public static UpdateDeviceCommand toCommandFromResource(String profileId, UpdateDeviceResource updateDeviceResource) {
        return new UpdateDeviceCommand(profileId, updateDeviceResource.deviceName(), updateDeviceResource.deviceConnectionAddress());
    }
}
