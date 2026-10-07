package com.dev.vacfy.monitoring.domain.model.aggregates;

import com.dev.vacfy.monitoring.domain.model.valueobjects.TemperatureLimits;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * Vacuna del catálogo (tabla vaccine_profiles): para qué sirve y en qué rango se conserva.
 * Las columnas agregadas después son opcionales en la base de datos: null equivale a false.
 */
@Entity
@Table(name = "vaccine_profiles")
@Getter
public class VaccineProfile {
    public static final double DEFAULT_MIN_TEMP = 2.0;
    public static final double DEFAULT_MAX_TEMP = 8.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Para qué sirve, en palabras de la enfermera (p. ej. "Sarampión, paperas y rubéola"). */
    @Column(name = "protects_against", length = 300)
    private String protectsAgainst;

    @Column(name = "min_temp", nullable = false)
    private Double minTemp;

    @Column(name = "max_temp", nullable = false)
    private Double maxTemp;

    /** true si la vacuna se daña por congelación (p. ej. las que contienen adyuvante de aluminio). */
    @Column(name = "freeze_sensitive", nullable = false)
    private Boolean freezeSensitive;

    /** true si la vacuna pierde efecto con el calor (p. ej. las de virus vivos atenuados). */
    @Column(name = "heat_sensitive")
    private Boolean heatSensitive;

    @Column(name = "doses_per_vial")
    private Integer dosesPerVial;

    @Column(length = 1000)
    private String notes;

    /** false hasta que alguien revise los datos con la ficha técnica del fabricante. */
    private Boolean verified;

    protected VaccineProfile() { }

    public VaccineProfile(String name, double minTemp, double maxTemp, boolean freezeSensitive) {
        this(name, null, minTemp, maxTemp, freezeSensitive, false, null, null);
    }

    public VaccineProfile(String name, String protectsAgainst, double minTemp, double maxTemp,
                          boolean freezeSensitive, boolean heatSensitive, Integer dosesPerVial, String notes) {
        apply(name, protectsAgainst, minTemp, maxTemp, freezeSensitive, heatSensitive, dosesPerVial, notes);
        this.verified = false;
    }

    public void update(String name, String protectsAgainst, double minTemp, double maxTemp,
                       boolean freezeSensitive, boolean heatSensitive, Integer dosesPerVial, String notes,
                       boolean verified) {
        apply(name, protectsAgainst, minTemp, maxTemp, freezeSensitive, heatSensitive, dosesPerVial, notes);
        this.verified = verified;
    }

    private void apply(String name, String protectsAgainst, double minTemp, double maxTemp,
                       boolean freezeSensitive, boolean heatSensitive, Integer dosesPerVial, String notes) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("El nombre de la vacuna es obligatorio");
        if (name.trim().length() > 100) throw new IllegalArgumentException("El nombre admite hasta 100 caracteres");
        if (protectsAgainst != null && protectsAgainst.trim().length() > 300) {
            throw new IllegalArgumentException("\"Para qué sirve\" admite hasta 300 caracteres");
        }
        if (dosesPerVial != null && dosesPerVial < 1) throw new IllegalArgumentException("Las dosis por frasco deben ser 1 o más");
        if (notes != null && notes.trim().length() > 1000) throw new IllegalArgumentException("Las notas admiten hasta 1000 caracteres");
        new TemperatureLimits(name, minTemp, maxTemp, freezeSensitive); // valida el rango
        this.name = name.trim();
        this.protectsAgainst = blankToNull(protectsAgainst);
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.freezeSensitive = freezeSensitive;
        this.heatSensitive = heatSensitive;
        this.dosesPerVial = dosesPerVial;
        this.notes = blankToNull(notes);
    }

    public boolean isFreezeSensitive() {
        return Boolean.TRUE.equals(freezeSensitive);
    }

    public boolean isHeatSensitive() {
        return Boolean.TRUE.equals(heatSensitive);
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(verified);
    }

    public TemperatureLimits toLimits() {
        return new TemperatureLimits(name, minTemp, maxTemp, isFreezeSensitive(), isHeatSensitive(), java.util.List.of());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
