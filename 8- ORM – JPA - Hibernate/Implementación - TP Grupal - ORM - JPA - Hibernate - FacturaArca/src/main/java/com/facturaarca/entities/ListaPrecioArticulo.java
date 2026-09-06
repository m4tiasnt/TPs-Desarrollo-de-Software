package com.facturaarca.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "lista_precio_articulo")
public class ListaPrecioArticulo extends EntityId {

    @Column(name = "nombre")
    private String nombre;

    public ListaPrecioArticulo() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}