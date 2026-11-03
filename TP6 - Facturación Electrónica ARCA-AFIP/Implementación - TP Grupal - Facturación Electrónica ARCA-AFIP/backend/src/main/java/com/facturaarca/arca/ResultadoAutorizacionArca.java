package com.facturaarca.arca;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoAutorizacionArca {
    private boolean autorizado;
    private Long numeroComprobante;
    private Date fechaEmision;
    private String cae;
    private Date caeVencimiento;
    private String motivoRechazo;
    private String mensaje;
}
