package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clientes", schema = "model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class Cliente extends AuditoriaApp {

    @Column(name = "cuit_cuil", nullable = false, unique = true)
    @EqualsAndHashCode.Include
    private String cuitCuil;

    @Column(nullable = false)
    private String denominacion;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private Contacto contacto;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private Domicilio domicilio;
}