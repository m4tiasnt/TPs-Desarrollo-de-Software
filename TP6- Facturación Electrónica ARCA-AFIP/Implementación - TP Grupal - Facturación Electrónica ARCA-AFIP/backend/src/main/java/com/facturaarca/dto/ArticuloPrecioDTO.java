package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Artículo para agregar a una factura con el precio de la lista seleccionada.
 * Se usa en el buscador de productos de la UI al crear o editar facturas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPrecioDTO {

    private Long id;
    private String codigo;
    private String denominacion;
    private String marca;
    private Double precioVenta;
    private Long listaPrecioArticuloId;
    private Long listaPrecioId;
    private String listaPrecioDenominacion;
}
