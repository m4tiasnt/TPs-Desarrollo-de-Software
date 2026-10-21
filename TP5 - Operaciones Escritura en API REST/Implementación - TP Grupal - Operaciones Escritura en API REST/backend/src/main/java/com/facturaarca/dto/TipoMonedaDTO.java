package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoMonedaDTO {
    private Long id;
    private Integer codigoAfip;
    private String denominacion;
    private String simbolo;
}