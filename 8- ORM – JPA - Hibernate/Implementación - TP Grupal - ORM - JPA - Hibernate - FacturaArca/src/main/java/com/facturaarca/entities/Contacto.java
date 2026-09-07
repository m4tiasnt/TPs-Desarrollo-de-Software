package com.facturaarca.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "contactos")
public class Contacto extends EntityId {
    private String email;
    private String telefono;
    private String celular;


    public Contacto() {
    }

    public Contacto(String email, String telefono, String celular) {
        super();
        this.email = email;
        this.telefono = telefono;
        this.celular = celular;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }
}
