package com.facturaarca.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "listas_precio")
public class ListaPrecio extends AuditoriaApp {

     @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String denominacion;

    public ListaPrecio() {
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}