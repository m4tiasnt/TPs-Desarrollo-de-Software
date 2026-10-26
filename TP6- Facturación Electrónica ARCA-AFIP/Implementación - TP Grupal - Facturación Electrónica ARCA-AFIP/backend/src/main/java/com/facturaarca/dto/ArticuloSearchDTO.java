package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloSearchDTO {
    private Long id;
    private String codigo;
    private String denominacion;
    private String marca;
    private Double precioVenta;
    private Long listaPrecioArticuloId;
}