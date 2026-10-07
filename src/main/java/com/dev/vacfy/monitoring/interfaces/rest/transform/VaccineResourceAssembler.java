package com.dev.vacfy.monitoring.interfaces.rest.transform;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProfile;
import com.dev.vacfy.monitoring.interfaces.rest.resources.VaccineResource;

public final class VaccineResourceAssembler {
    private VaccineResourceAssembler() { }

    public static VaccineResource toResource(VaccineProfile vaccine) {
        if (vaccine == null) return null;
        return new VaccineResource(vaccine.getId(), vaccine.getName(), vaccine.getProtectsAgainst(),
                vaccine.getMinTemp(), vaccine.getMaxTemp(), vaccine.isFreezeSensitive(), vaccine.isHeatSensitive(),
                vaccine.getDosesPerVial(), vaccine.getNotes(), vaccine.isVerified());
    }
}
