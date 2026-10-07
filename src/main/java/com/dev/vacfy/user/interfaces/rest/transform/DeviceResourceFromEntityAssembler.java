package com.dev.vacfy.user.interfaces.rest.transform;

import com.dev.vacfy.user.domain.model.aggregates.Device;
import com.dev.vacfy.user.interfaces.rest.resources.DeviceResource;

public class DeviceResourceFromEntityAssembler {
    public static DeviceResource toResourceFromEntity(Device entity) {
        return new DeviceResource(entity.getDeviceId().deviceId().toString(), entity.getDeviceName().deviceName(), entity.getDeviceConnectionAddress().connectionAddress());
    }
}
