package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "listas_precio", schema = "model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class ListaPrecio extends AuditoriaApp {

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private String codigo;

    @Column(nullable = false)
    private String denominacion;
}