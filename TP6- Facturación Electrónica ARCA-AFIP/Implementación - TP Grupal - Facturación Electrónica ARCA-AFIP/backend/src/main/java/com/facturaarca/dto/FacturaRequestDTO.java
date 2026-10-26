package com.facturaarca.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaRequestDTO {

    private Long numero;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "America/Argentina/Buenos_Aires")
    private Date fechaEmision;

    private Long clienteId;
    private Long condicionIvaId;
    private Long puntoVentaId;
    private Long tipoMonedaId;
    private String estado;
    private String observaciones;

    @Builder.Default
    private List<FacturaDetalleRequestDTO> detalles = new ArrayList<>();
}
