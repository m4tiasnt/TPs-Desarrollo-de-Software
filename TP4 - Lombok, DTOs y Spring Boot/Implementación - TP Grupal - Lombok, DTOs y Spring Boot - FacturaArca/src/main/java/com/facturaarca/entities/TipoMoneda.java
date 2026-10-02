package com.facturaarca.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipos_moneda", schema = "model")
public class TipoMoneda extends AuditoriaApp {

    @Column(name = "codigo_afip", nullable = false, unique = true)
    private Integer codigoAfip;

    @Column(nullable = false)
    private String denominacion;

    @Column(nullable = false)
    private String simbolo;


    public TipoMoneda() {
    }

    public TipoMoneda(Integer codigoAfip, String denominacion, String simbolo) {
        super();
        this.codigoAfip = codigoAfip;
        this.denominacion = denominacion;
        this.simbolo = simbolo;
    }

    public Integer getCodigoAfip() {
        return codigoAfip;
    }

    public void setCodigoAfip(Integer codigoAfip) {
        this.codigoAfip = codigoAfip;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public void setSimbolo(String simbolo) {
        this.simbolo = simbolo;
    }
}
