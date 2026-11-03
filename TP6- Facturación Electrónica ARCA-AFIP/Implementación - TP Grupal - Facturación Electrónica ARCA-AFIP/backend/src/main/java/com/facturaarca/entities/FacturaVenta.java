package com.facturaarca.entities;

import com.facturaarca.enums.EstadoFactura;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private Long numero;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFactura estado;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaAnulacion;

    private String observaciones;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
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

    public void actualizarDatos(String nuevasObservaciones, Cliente nuevoCliente,
                                CondicionIva nuevaCondicionIva, PuntoVenta nuevoPuntoVenta,
                                TipoMoneda nuevoTipoMoneda, Usuario usuarioModificacion) {
        if (nuevasObservaciones != null) {
            this.observaciones = nuevasObservaciones;
        }
        this.cliente = nuevoCliente;
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

    public void marcarEnProceso(Usuario usuarioModificacion) {
        this.estado = EstadoFactura.EN_PROCESO;
        this.fechaModificacion = new Date();
        if (usuarioModificacion != null) {
            this.usuarioModificacion = usuarioModificacion;
        }
    }

    public void autorizar(Long numero, Date fechaEmision, String cae, Date caeFechaVencimiento,
                          Usuario usuarioModificacion) {
        this.numero = numero;
        this.fechaEmision = fechaEmision;
        this.cae = cae;
        this.caeFechaVencimiento = caeFechaVencimiento;
        this.resultadoAfip = "A";
        this.motivoRechazo = null;
        this.estado = EstadoFactura.AUTORIZADA;
        this.fechaModificacion = new Date();
        if (usuarioModificacion != null) {
            this.usuarioModificacion = usuarioModificacion;
        }
    }

    public void rechazar(String motivo, Usuario usuarioModificacion) {
        this.motivoRechazo = motivo;
        this.resultadoAfip = "R";
        this.estado = EstadoFactura.RECHAZADA;
        this.fechaModificacion = new Date();
        if (usuarioModificacion != null) {
            this.usuarioModificacion = usuarioModificacion;
        }
    }

    public void anular(Usuario usuarioModificacion) {
        this.estado = EstadoFactura.ANULADA;
        this.fechaAnulacion = new Date();
        this.fechaModificacion = new Date();
        if (usuarioModificacion != null) {
            this.usuarioModificacion = usuarioModificacion;
        }
    }
}
