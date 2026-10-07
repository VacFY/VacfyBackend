package com.dev.vacfy.user.domain.services;

import com.dev.vacfy.user.domain.model.aggregates.Device;
import com.dev.vacfy.user.domain.model.queries.GetDeviceByUserIdQuery;

import java.util.Optional;

public interface DeviceQueryService {
    Optional<Device> handle(GetDeviceByUserIdQuery getDeviceByUserIdQuery);
}
