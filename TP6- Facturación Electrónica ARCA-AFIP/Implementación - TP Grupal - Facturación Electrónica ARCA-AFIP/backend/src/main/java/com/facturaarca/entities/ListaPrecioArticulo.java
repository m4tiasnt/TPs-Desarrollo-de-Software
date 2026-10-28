package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lista_precio_articulo", schema = "model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class ListaPrecioArticulo extends AuditoriaApp {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id", nullable = false)
    private ListaPrecio listaPrecio;

    @Column(nullable = false)
    private double precioVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articulo_id", nullable = false)
    private Articulo articulo;
}