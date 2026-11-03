package com.facturaarca.dto;

import com.facturaarca.enums.EstadoFactura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Resultado de la autorización fiscal de una factura ante ARCA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAutorizacionDTO {

    private Long id;
    private Long numero;
    private EstadoFactura estado;
    private String cae;
    private Date caeFechaVencimiento;
    private Date fechaEmision;
    private String motivoRechazo;
    private String mensaje;
}
