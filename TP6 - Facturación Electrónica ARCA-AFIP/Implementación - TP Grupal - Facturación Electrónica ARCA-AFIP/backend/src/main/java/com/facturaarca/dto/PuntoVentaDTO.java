package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuntoVentaDTO {
    private Long id;
    private int numero;
    private String descripcion;
}