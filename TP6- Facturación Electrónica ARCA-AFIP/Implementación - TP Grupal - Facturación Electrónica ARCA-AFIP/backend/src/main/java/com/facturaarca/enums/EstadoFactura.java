package com.facturaarca.enums;

/**
 * Estados posibles de una factura de venta.
 */
public enum EstadoFactura {

    BORRADOR,
    EN_PROCESO,
    AUTORIZADA,
    RECHAZADA,
    ANULADA;

    public boolean esEditable() {
        return this == BORRADOR || this == RECHAZADA;
    }

    public boolean esInformable() {
        return this == BORRADOR || this == RECHAZADA;
    }

    public boolean esAnulable() {
        return this == BORRADOR || this == RECHAZADA;
    }

    public boolean esEliminable() {
        return this == BORRADOR;
    }
}
