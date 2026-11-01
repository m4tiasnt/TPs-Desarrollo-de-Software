package com.facturaarca.dto;

import com.facturaarca.enums.EstadoFactura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Esta clase se utiliza para generar los DTOs que se utilizan
 * para generar los reportes. Sólo contiene los datos que se
 * muestran en ellos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaVentaReporteDTO {
    private Long id;
    private Long numeroFactura;
    private Date fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private double importeTotal;
    private Long cantidadItems;
    private EstadoFactura estado;
}