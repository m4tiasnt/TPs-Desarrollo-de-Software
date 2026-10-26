package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Precio de venta de un artículo en una lista de precios concreta.
 * Lo consume el frontend para que el usuario elija la lista de precios del renglón.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListaPrecioArticuloDTO {

    private Long id;
    private Long listaPrecioId;
    private String listaPrecio;
    private double precioVenta;
}
