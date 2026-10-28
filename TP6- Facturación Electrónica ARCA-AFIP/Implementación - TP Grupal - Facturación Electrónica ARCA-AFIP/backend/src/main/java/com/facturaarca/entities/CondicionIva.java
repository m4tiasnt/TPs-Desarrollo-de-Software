package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "condiciones_iva", schema = "model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class CondicionIva extends AuditoriaApp {

    @Column(name = "codigo_afip", nullable = false, unique = true)
    @EqualsAndHashCode.Include
    private int codigoAfip;

    @Column(nullable = false)
    private String denominacion;
}