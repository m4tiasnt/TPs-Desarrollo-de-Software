package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_moneda") // MODIFICADO: se quitó schema = "model", la tabla pasa al esquema "public" por defecto
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class TipoMoneda extends AuditoriaApp {

    @Column(name = "codigo_afip", nullable = false, unique = true)
    @EqualsAndHashCode.Include
    private Integer codigoAfip;

    @Column(nullable = false)
    private String denominacion;

    @Column(nullable = false)
    private String simbolo;
}