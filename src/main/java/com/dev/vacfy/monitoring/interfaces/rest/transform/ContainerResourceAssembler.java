package com.dev.vacfy.monitoring.interfaces.rest.transform;

import com.dev.vacfy.monitoring.domain.model.valueobjects.IssuedKey;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ContainerKeyResource;

public final class ContainerResourceAssembler {
    private ContainerResourceAssembler() { }

    public static ContainerKeyResource toResource(IssuedKey issued) {
        var container = issued.container();
        return new ContainerKeyResource(container.getCodigo(), container.getNombre(), issued.clave(), container.isActivo(),
                container.getCreadoEn().toString());
    }
}
