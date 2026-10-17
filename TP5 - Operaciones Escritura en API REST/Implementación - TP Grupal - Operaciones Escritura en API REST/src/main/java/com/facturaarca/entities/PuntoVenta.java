package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puntos_venta", schema = "model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class PuntoVenta extends AuditoriaApp {

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private int numero;

    private String descripcion;

    @Column(name = "tipo_emision")
    private String tipoEmision;

    @Column(name = "domicilio_comercial")
    private String domicilioComercial;
}