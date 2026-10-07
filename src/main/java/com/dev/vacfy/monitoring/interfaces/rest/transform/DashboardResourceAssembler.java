package com.dev.vacfy.monitoring.interfaces.rest.transform;

import com.dev.vacfy.monitoring.domain.model.valueobjects.ContainerSummary;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotView;
import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;
import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource.ContainerSummaryResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource.NextExpiryResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.DashboardSummaryResource.RangeResource;

import java.time.Instant;
import java.util.List;

public final class DashboardResourceAssembler {
    private DashboardResourceAssembler() { }

    public static DashboardSummaryResource toResource(List<ContainerSummary> summaries, Instant generatedAt) {
        return new DashboardSummaryResource(generatedAt.toString(),
                summaries.stream().map(DashboardResourceAssembler::toResource).toList());
    }

    private static ContainerSummaryResource toResource(ContainerSummary summary) {
        return new ContainerSummaryResource(summary.contenedor(), summary.status().name(), summary.temperatura(),
                summary.humedad(), summary.lastReadingAt() == null ? null : summary.lastReadingAt().toString(),
                toResource(summary.limits()), summary.activeLots(), summary.expiredLots(), toResource(summary.nextExpiry()),
                summary.openAlerts(), summary.highestSeverity() == null ? null : summary.highestSeverity().name());
    }

    private static RangeResource toResource(TemperatureLimits limits) {
        boolean lots = limits.basedOnLots();
        return new RangeResource(limits.minTemp(), limits.maxTemp(), lots ? "LOTS" : "PROFILE",
                lots ? null : limits.profileName(), limits.freezeSensitive(), limits.heatSensitive());
    }

    private static NextExpiryResource toResource(LotView view) {
        if (view == null) return null;
        var lot = view.lot();
        return new NextExpiryResource(lot.getId(), view.vaccine() == null ? null : view.vaccine().getName(),
                lot.getLotNumber(), lot.getExpiryDate().toString(), view.daysToExpiry());
    }
}
