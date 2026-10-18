package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "articulos") // MODIFICADO: se quitó schema = "model", la tabla pasa al esquema "public" por defecto
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class Articulo extends AuditoriaApp {

    @Column(nullable = false)
    private String denominacion;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private String codigo;

    @ManyToOne
    private Rubro rubro;

    @ManyToOne
    private Marca marca;
}