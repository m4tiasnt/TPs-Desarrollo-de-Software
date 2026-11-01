package com.facturaarca.dto;

import com.facturaarca.enums.EstadoFactura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Resultado de informar una factura a ARCA.
 * Mientras la integración real no esté conectada, la factura queda EN_PROCESO
 * y los campos de CAE/número/fecha siguen vacíos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformarArcaResultadoDTO {

    private Long id;
    private Long nroFactura;
    private EstadoFactura estado;
    private String cae;
    private Date caeFechaVencimiento;
    private Date fechaEmision;
    private String motivoRechazo;
    private String mensaje;
}
