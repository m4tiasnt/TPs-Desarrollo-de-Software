package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa cada detalle de una factura. Suele estar dentro
 * de un objeto FacturaVentaDTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaVentaDetalleDTO {

    private Long id;
    private Long articuloId;
    private String codigoArticulo;
    private String denominacionArticulo;
    private Long listaPrecioArticuloId;
    private String listaPrecio;
    private String descripcion;
    private double cantidad;
    private double precioUnitario;
    private double porcentajeBonificacion;
    private double importeNeto;
    private double importeIva;
    private double importeSubtotal;
}
