package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Cómo se cuida una vacuna en el termo: el criterio que la enfermera debe priorizar. */
public enum CareProfile {
    /** Se daña si se congela: paquetes fríos acondicionados y nada de contacto directo con hielo. */
    ANTI_FREEZE("Anticongelamiento"),
    /** Pierde efecto con la luz y el calor. */
    PROTECT_FROM_LIGHT_AND_HEAT("Proteger de luz y calor"),
    /** Depende del producto: revisar la ficha técnica del fabricante. */
    CHECK_MANUFACTURER("Verificar fabricante");

    private final String label;

    CareProfile(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
