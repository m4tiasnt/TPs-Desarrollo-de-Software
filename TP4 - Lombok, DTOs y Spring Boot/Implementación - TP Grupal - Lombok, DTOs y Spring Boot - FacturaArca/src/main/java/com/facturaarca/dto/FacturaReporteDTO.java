package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaReporteDTO {
    private Long id;
    private String letra;
    private Integer numero;
    private String razonSocialCliente;
    private Double total;
}
