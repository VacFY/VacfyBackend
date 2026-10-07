package com.dev.vacfy.user.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.user.domain.model.aggregates.Device;
import com.dev.vacfy.user.domain.model.values.DeviceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, DeviceId> {
    Optional<Device> findByDeviceUserId_UserId(UUID deviceUserId);
    Optional<Device> findByDeviceUserId_UserIdAndDeviceId_DeviceId(UUID deviceUserId, UUID deviceId);
}
