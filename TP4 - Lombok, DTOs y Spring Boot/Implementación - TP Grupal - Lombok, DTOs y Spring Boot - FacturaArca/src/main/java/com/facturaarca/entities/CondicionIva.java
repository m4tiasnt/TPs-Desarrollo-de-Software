package com.facturaarca.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "condiciones_iva", schema = "model")
public class CondicionIva extends AuditoriaApp {

    @Column(name = "codigo_afip", nullable = false, unique = true)
    private int codigoAfip;

    @Column(nullable = false)
    private String denominacion;


    public CondicionIva() {
    }

    public CondicionIva(int codigoAfip, String denominacion) {
        super();
        this.codigoAfip = codigoAfip;
        this.denominacion = denominacion;
    }

    public int getCodigoAfip() {
        return codigoAfip;
    }

    public void setCodigoAfip(int codigoAfip) {
        this.codigoAfip = codigoAfip;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }
}
