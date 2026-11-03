package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaPrecioDTO {
    private Long id;
    private String codigo;
    private String denominacion;
}