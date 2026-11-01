package com.facturaarca.dto;

import com.facturaarca.enums.EstadoFactura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * Detalle completo de una factura: cabecera, importes calculados y detalles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaVentaDTO {

    private Long id;
    private Long numero;
    private String numeroComprobante;
    private Date fechaEmision;

    private Long clienteId;
    private String clienteDenominacion;
    private String clienteCuitCuil;

    private Long condicionIvaId;
    private String condicionIva;

    private Long puntoVentaId;
    private Integer puntoVentaNumero;
    private String puntoVentaDescripcion;

    private Long tipoMonedaId;
    private String tipoMoneda;
    private String tipoMonedaSimbolo;

    private double importeNeto;
    private double importeIva;
    private double importeTotal;
    private double importeCobrado;
    private double importeSaldo;

    private EstadoFactura estado;
    private String cae;
    private Date caeFechaVencimiento;
    private String resultadoAfip;
    private String motivoRechazo;
    private String observaciones;

    private Date fechaAlta;
    private Date fechaModificacion;
    private Date fechaAnulacion;

    private List<FacturaVentaDetalleDTO> detalles;
}
