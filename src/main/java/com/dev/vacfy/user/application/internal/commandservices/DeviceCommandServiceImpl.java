package com.dev.vacfy.user.application.internal.commandservices;

import com.dev.vacfy.user.domain.exceptions.DeviceNotFoundException;
import com.dev.vacfy.user.domain.model.aggregates.Device;
import com.dev.vacfy.user.domain.model.commands.CreateDeviceCommand;
import com.dev.vacfy.user.domain.model.commands.DeleteDeviceCommand;
import com.dev.vacfy.user.domain.model.commands.UpdateDeviceCommand;
import com.dev.vacfy.user.domain.services.DeviceCommandService;
import com.dev.vacfy.user.infrastructure.persistence.jpa.repositories.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {
    private final DeviceRepository deviceRepository;

    public DeviceCommandServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public void handle(CreateDeviceCommand createDeviceCommand) {
        var device = new Device(createDeviceCommand.deviceUserId(), createDeviceCommand.deviceName(), createDeviceCommand.deviceConnectionAddress());
        deviceRepository.save(device);
    }

    @Override
    public void handle(UpdateDeviceCommand updateDeviceCommand) {
        var device = deviceRepository.findByDeviceUserId_UserId(UUID.fromString(updateDeviceCommand.deviceUserId()));
        if (device.isEmpty()) throw new DeviceNotFoundException(updateDeviceCommand.deviceUserId());
        var deviceToUpdate = device.get();
        deviceRepository.save(deviceToUpdate.updateDevice(updateDeviceCommand.deviceName(), updateDeviceCommand.deviceConnectionAddress()));
    }

    @Override
    public void handle(DeleteDeviceCommand deleteDeviceCommand) {
        var device = deviceRepository.findByDeviceUserId_UserIdAndDeviceId_DeviceId(UUID.fromString(deleteDeviceCommand.profileId()), UUID.fromString(deleteDeviceCommand.deviceId()));
        if (device.isEmpty()) throw new DeviceNotFoundException(deleteDeviceCommand.profileId());
        deviceRepository.delete(device.get());
    }
}
