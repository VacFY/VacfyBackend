package com.dev.vacfy.monitoring.interfaces.rest.transform;

import com.dev.vacfy.monitoring.domain.model.valueobjects.LotCodeReading;
import com.dev.vacfy.monitoring.domain.model.valueobjects.LotView;
import com.dev.vacfy.monitoring.interfaces.rest.resources.CodeReadingResource;
import com.dev.vacfy.monitoring.interfaces.rest.resources.LotResource;

import java.time.Instant;
import java.time.LocalDate;

public final class LotResourceAssembler {
    private LotResourceAssembler() { }

    public static LotResource toResource(LotView view) {
        var lot = view.lot();
        return new LotResource(lot.getId(), lot.getContenedor(), VaccineResourceAssembler.toResource(view.vaccine()),
                lot.getGtin(), lot.getLotNumber(), iso(lot.getExpiryDate()), view.daysToExpiry(), lot.getVials(),
                lot.getDoses(), lot.getSource().name(), lot.getStatus().name(), lot.getRegisteredBy(),
                iso(lot.getRegisteredAt()), iso(lot.getClosedAt()), lot.getCloseReason());
    }

    public static CodeReadingResource toResource(LotCodeReading reading) {
        var data = reading.data();
        return new CodeReadingResource(data.gtin(), data.lot(), iso(data.expiry()), data.serial(),
                reading.daysToExpiry(), VaccineResourceAssembler.toResource(reading.vaccine()),
                reading.knownProduct(), reading.warnings());
    }

    private static String iso(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
