package com.dev.vacfy.monitoring.interfaces.rest.transform;

import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;
import com.dev.vacfy.monitoring.domain.model.aggregates.ContainerProfile;
import com.dev.vacfy.monitoring.domain.model.aggregates.Reading;
import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.domain.services.AlertTexts;
import com.dev.vacfy.monitoring.interfaces.rest.resources.AffectedLotResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.AlertResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ContainerProfileResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ReadingResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.VaccineProfileResource;

import java.time.Instant;
import java.util.List;

public final class MonitoringResourceAssembler {
    private MonitoringResourceAssembler() { }

    public static AlertResource toResource(Alert alert) {
        return new AlertResource(
                alert.getId(),
                alert.getContenedor(),
                alert.getType().name(),
                alert.getSeverity().name(),
                alert.getStatus().name(),
                alert.getMessage(),
                alert.getTriggerValue(),
                alert.getMinValue(),
                alert.getMaxValue(),
                iso(alert.getStartedAt()),
                iso(alert.getAcknowledgedAt()),
                alert.getAcknowledgedBy(),
                iso(alert.getResolvedAt()),
                alert.getResolutionMessage(),
                alert.getTitle() != null ? alert.getTitle() : AlertTexts.fallbackTitle(alert.getType(), alert.getContenedor()),
                alert.getAffectedLots() == null ? List.of() : alert.getAffectedLots().stream()
                        .map(lot -> new AffectedLotResource(lot.lotId(), lot.vaccine(), lot.lotNumber(), lot.expiryDate()))
                        .toList(),
                alert.getLotId());
    }

    public static ReadingResource toResource(Reading reading) {
        return new ReadingResource(reading.getId(), reading.getContenedor(), reading.getTemperatura(),
                reading.getHumedad(), iso(reading.getReceivedAt()));
    }

    public static VaccineProfileResource toResource(VaccineProfile profile) {
        return new VaccineProfileResource(profile.getId(), profile.getName(), profile.getMinTemp(),
                profile.getMaxTemp(), profile.getFreezeSensitive());
    }

    public static ContainerProfileResource toResource(ContainerProfile containerProfile) {
        return new ContainerProfileResource(containerProfile.getContenedor(), containerProfile.getProfileId());
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
