package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "factura_venta", schema = "model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class FacturaVenta extends AuditoriaApp {

    @EqualsAndHashCode.Include
    private Long numero;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condicion_iva_id", nullable = false)
    private CondicionIva condicionIva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_moneda_id", nullable = false)
    private TipoMoneda tipoMoneda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_venta_id", nullable = false)
    private PuntoVenta puntoVenta;

    private double importeCobrado;

    private double importeSaldo;

    @Column(nullable = false)
    private double importeTotal;

    private String cae;

    @Temporal(TemporalType.DATE)
    private Date caeFechaVencimiento;

    private String resultadoAfip;

    private String motivoRechazo;

    @Column(nullable = false)
    private String estado;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaAnulacion;

    private String observaciones;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<FacturaVentaDetalle> detalles = new ArrayList<>();

    public void addDetalle(FacturaVentaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFactura(this);
    }

    public void removeDetalle(FacturaVentaDetalle detalle) {
        detalles.remove(detalle);
        detalle.setFactura(null);
    }

    public void clearDetalles() {
        for (FacturaVentaDetalle detalle : new ArrayList<>(this.detalles)) {
            removeDetalle(detalle);
        }
    }

    public void recalcularTotales() {
        double total = 0.0;
        if (detalles != null) {
            for (FacturaVentaDetalle detalle : detalles) {
                detalle.calcularImportes();
                total += detalle.getImporteSubtotal();
            }
        }
        this.importeTotal = Math.round(total * 100.0) / 100.0;
        this.importeCobrado = this.importeTotal;
        this.importeSaldo = 0.0;
    }

    public void darDeBaja(Usuario usuarioBaja) {
        Date ahora = new Date();
        this.fechaBaja = ahora;
        this.fechaModificacion = ahora;
        if (usuarioBaja != null) {
            this.usuarioBaja = usuarioBaja;
            this.usuarioModificacion = usuarioBaja;
        }
    }

    public void actualizarDatos(String nuevoEstado, String nuevasObservaciones, Date nuevaFechaEmision,
                                Cliente nuevoCliente, CondicionIva nuevaCondicionIva,
                                PuntoVenta nuevoPuntoVenta, TipoMoneda nuevoTipoMoneda,
                                Usuario usuarioModificacion) {
        if (nuevoEstado != null && !nuevoEstado.trim().isEmpty()) {
            this.estado = nuevoEstado;
            if ("ANULADA".equalsIgnoreCase(nuevoEstado) && this.fechaAnulacion == null) {
                this.fechaAnulacion = new Date();
            }
        }
        if (nuevasObservaciones != null) {
            this.observaciones = nuevasObservaciones;
        }
        if (nuevaFechaEmision != null) {
            this.fechaEmision = nuevaFechaEmision;
        }
        if (nuevoCliente != null) {
            this.cliente = nuevoCliente;
        }
        if (nuevaCondicionIva != null) {
            this.condicionIva = nuevaCondicionIva;
        }
        if (nuevoPuntoVenta != null) {
            this.puntoVenta = nuevoPuntoVenta;
        }
        if (nuevoTipoMoneda != null) {
            this.tipoMoneda = nuevoTipoMoneda;
        }
        this.fechaModificacion = new Date();
        if (usuarioModificacion != null) {
            this.usuarioModificacion = usuarioModificacion;
        }
    }
}