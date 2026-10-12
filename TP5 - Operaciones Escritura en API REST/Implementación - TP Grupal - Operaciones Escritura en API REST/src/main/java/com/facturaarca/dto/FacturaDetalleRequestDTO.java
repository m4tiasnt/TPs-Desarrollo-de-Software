package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDetalleRequestDTO {

    private Long articuloId;
    private Long listaPrecioArticuloId;
    private String descripcion;
    private double cantidad;
    private Double precioUnitario;
    private Double porcentajeBonificacion;
    private Double importeSubtotal;
}
