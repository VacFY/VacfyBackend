package com.dev.vacfy.user.application.internal.queryservices;

import com.dev.vacfy.user.domain.model.aggregates.Device;
import com.dev.vacfy.user.domain.model.queries.GetDeviceByUserIdQuery;
import com.dev.vacfy.user.domain.services.DeviceQueryService;
import com.dev.vacfy.user.infrastructure.persistence.jpa.repositories.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceQueryServiceImpl implements DeviceQueryService {
    private final DeviceRepository deviceRepository;

    public DeviceQueryServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Optional<Device> handle(GetDeviceByUserIdQuery getDeviceByUserIdQuery) {
        return deviceRepository.findByDeviceUserId_UserId(UUID.fromString(getDeviceByUserIdQuery.deviceUserId()));
    }
}
