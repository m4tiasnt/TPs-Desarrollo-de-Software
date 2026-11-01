package com.facturaarca.dto;

import com.facturaarca.enums.EstadoFactura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Representa cada fila del listado de facturas del frontend.
 * Solo trae las columnas que muestra la tabla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaVentaListadoDTO {

    private Long id;
    private Long numero;
    private String numeroComprobante;
    private Date fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private Integer puntoVentaNumero;
    private String puntoVentaDescripcion;
    private double importeTotal;
    private Long cantidadItems;
    private EstadoFactura estado;

    /**
     * Constructor que usa la consulta JPQL del listado.
     * Calcula el número de comprobante formateado (0001-00000042).
     */
    public FacturaVentaListadoDTO(Long id, Long numero, Date fechaEmision,
                             String clienteDenominacion, String condicionIva,
                             Integer puntoVentaNumero, String puntoVentaDescripcion,
                             double importeTotal, Long cantidadItems, EstadoFactura estado) {
        this.id = id;
        this.numero = numero;
        this.fechaEmision = fechaEmision;
        this.clienteDenominacion = clienteDenominacion;
        this.condicionIva = condicionIva;
        this.puntoVentaNumero = puntoVentaNumero;
        this.puntoVentaDescripcion = puntoVentaDescripcion;
        this.importeTotal = importeTotal;
        this.cantidadItems = cantidadItems;
        this.estado = estado;
        this.numeroComprobante = formatearNumeroComprobante(puntoVentaNumero, numero);
    }

    private static String formatearNumeroComprobante(Integer puntoVentaNumero, Long numero) {
        if (puntoVentaNumero == null || numero == null) {
            return null;
        }
        return String.format("%04d-%08d", puntoVentaNumero, numero);
    }
}
