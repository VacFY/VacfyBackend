package com.dev.vacfy.monitoring.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Termo registrado por la microred. El código es el que envía el ESP32 ("001"); la clave de vinculación
 * solo se guarda como hash. Opcionalmente tiene un código temporal (emparejamiento por QR) que vence a los minutos.
 */
@Entity
@Table(name = "containers")
@Getter
public class Container {
    @Id
    @Column(length = 64)
    private String codigo;

    @Column(length = 100)
    private String nombre;

    @Column(name = "clave_hash", nullable = false, length = 100)
    private String claveHash;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(nullable = false)
    private Boolean activo;

    /** Hash del código temporal vigente (fase OLED/QR); null si no hay. */
    @Column(name = "codigo_temporal_hash", length = 100)
    private String codigoTemporalHash;

    @Column(name = "codigo_temporal_vence")
    private Instant codigoTemporalVence;

    protected Container() { }

    public Container(String codigo, String nombre, String claveHash, Instant creadoEn) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.claveHash = claveHash;
        this.creadoEn = creadoEn;
        this.activo = true;
    }

    public boolean isActivo() {
        return Boolean.TRUE.equals(activo);
    }

    public void changeKey(String claveHash) {
        this.claveHash = claveHash;
    }

    /** Reemplaza el código temporal anterior: solo hay uno vigente por termo. */
    public void issueTemporaryCode(String hash, Instant vence) {
        this.codigoTemporalHash = hash;
        this.codigoTemporalVence = vence;
    }

    public boolean hasTemporaryCode(Instant now) {
        return codigoTemporalHash != null && codigoTemporalVence != null && now.isBefore(codigoTemporalVence);
    }

    public void clearTemporaryCode() {
        this.codigoTemporalHash = null;
        this.codigoTemporalVence = null;
    }
}
