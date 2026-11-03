package com.facturaarca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CondicionIvaDTO {
    private Long id;
    private int codigoAfip;
    private String denominacion;
}