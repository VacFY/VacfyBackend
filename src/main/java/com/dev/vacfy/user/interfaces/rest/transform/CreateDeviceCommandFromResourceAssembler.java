package com.dev.vacfy.user.interfaces.rest.transform;

import com.dev.vacfy.user.domain.model.commands.CreateDeviceCommand;
import com.dev.vacfy.user.interfaces.rest.resources.CreateDeviceResource;

public class CreateDeviceCommandFromResourceAssembler {
    public static CreateDeviceCommand toCommandFromResource(String profileId, CreateDeviceResource createDeviceResource) {
        return new CreateDeviceCommand(profileId, createDeviceResource.deviceName(), createDeviceResource.deviceConnectionAddress());
    }
}
