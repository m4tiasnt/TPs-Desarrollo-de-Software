package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "factura_venta_detalle", schema = "model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class FacturaVentaDetalle extends EntityId {

    @ManyToOne
    @JoinColumn(name = "factura_venta_id", nullable = false)
    @ToString.Exclude
    private FacturaVenta factura;

    @ManyToOne
    @JoinColumn(name = "lista_precio_articulo_id", nullable = false)
    private ListaPrecioArticulo listaPrecioArticulo;

    private String descripcion;

    @Column(nullable = false)
    private double cantidad;

    @Column(nullable = false)
    private double precioUnitario;

    private double porcentajeBonificacion;

    private double importeNeto;

    private double importeIva;

    @Column(nullable = false)
    private double importeSubtotal;

    public void calcularImportes() {
        double cant = this.cantidad > 0 ? this.cantidad : 1.0;
        this.cantidad = cant;

        if (this.precioUnitario <= 0 && this.listaPrecioArticulo != null) {
            this.precioUnitario = this.listaPrecioArticulo.getPrecioVenta();
        }

        double bonif = this.porcentajeBonificacion > 0 ? this.porcentajeBonificacion : 0.0;
        this.porcentajeBonificacion = bonif;

        double subtotal = cant * this.precioUnitario * (1.0 - (bonif / 100.0));
        double neto = Math.round((subtotal / 1.21) * 100.0) / 100.0;
        double iva = Math.round((subtotal - neto) * 100.0) / 100.0;

        this.importeSubtotal = Math.round(subtotal * 100.0) / 100.0;
        this.importeNeto = neto;
        this.importeIva = iva;
    }

    public static FacturaVentaDetalle crear(ListaPrecioArticulo lpa, String descripcion, double cantidad, Double porcentajeBonificacion) {
        FacturaVentaDetalle detalle = new FacturaVentaDetalle();
        detalle.setListaPrecioArticulo(lpa);
        detalle.setDescripcion(descripcion != null && !descripcion.trim().isEmpty()
                ? descripcion
                : (lpa.getArticulo() != null ? lpa.getArticulo().getDenominacion() : "Detalle"));
        detalle.setCantidad(cantidad > 0 ? cantidad : 1.0);
        detalle.setPrecioUnitario(lpa.getPrecioVenta());
        detalle.setPorcentajeBonificacion(porcentajeBonificacion != null ? porcentajeBonificacion : 0.0);
        detalle.calcularImportes();
        return detalle;
    }
}