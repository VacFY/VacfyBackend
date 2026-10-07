package com.dev.vacfy.user.domain.model.aggregates;

import com.dev.vacfy.user.domain.model.values.DeviceConnectionAddress;
import com.dev.vacfy.user.domain.model.values.DeviceId;
import com.dev.vacfy.user.domain.model.values.DeviceName;
import com.dev.vacfy.user.domain.model.values.DeviceUserId;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
public class Device {
    @EmbeddedId
    private DeviceId deviceId;

    @Embedded
    private DeviceUserId deviceUserId;

    @Embedded
    private DeviceName deviceName;

    @Embedded
    private DeviceConnectionAddress deviceConnectionAddress;

    public Device() {}

    public Device(String deviceUserId, String deviceName, String deviceConnectionAddress) {
        this.deviceId = new DeviceId();
        this.deviceUserId = new DeviceUserId(UUID.fromString(deviceUserId));
        this.deviceName = new DeviceName(deviceName);
        this.deviceConnectionAddress = new DeviceConnectionAddress(deviceConnectionAddress);
    }

    public Device updateDevice(String deviceName, String deviceConnectionAddress) {
        this.deviceName = new DeviceName(deviceName);
        this.deviceConnectionAddress = new DeviceConnectionAddress(deviceConnectionAddress);
        return this;
    }
}
