package com.facturaarca.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente extends AuditoriaApp {

    @Column(name = "cuit_cuil", nullable = false, unique = true)
    private String cuitCuil;

    @Column(nullable = false)
    private String denominacion;

    @OneToOne
    @JoinColumn(nullable = false)
    private Contacto contacto;

    @OneToOne
    @JoinColumn(nullable = false)
    private Domicilio domicilio;


    public Cliente() {
    }

    public Cliente(String cuitCuil, String denominacion, Contacto contacto, Domicilio domicilio) {
        super();
        this.cuitCuil = cuitCuil;
        this.denominacion = denominacion;
        this.contacto = contacto;
        this.domicilio = domicilio;
    }

    public String getCuitCuil() {
        return cuitCuil;
    }

    public void setCuitCuil(String cuitCuil) {
        this.cuitCuil = cuitCuil;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public Contacto getContacto() {
        return contacto;
    }

    public void setContacto(Contacto contacto) {
        this.contacto = contacto;
    }

    public Domicilio getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(Domicilio domicilio) {
        this.domicilio = domicilio;
    }
}
