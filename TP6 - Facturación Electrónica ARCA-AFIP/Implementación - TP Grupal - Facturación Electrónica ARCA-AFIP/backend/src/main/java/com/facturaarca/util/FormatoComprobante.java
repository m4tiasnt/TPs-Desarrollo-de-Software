package com.facturaarca.util;

/**
 * Formateo estándar de comprobantes fiscales según RG 4291 de ARCA/AFIP
 * (Punto de Venta de 5 dígitos + Número de Comprobante de 8 dígitos).
 */
public final class FormatoComprobante {

    private FormatoComprobante() {
    }

    public static String formatearPuntoVenta(Integer puntoVenta) {
        int pv = puntoVenta != null ? Math.max(1, puntoVenta) : 1;
        return String.format("%05d", pv);
    }

    public static String formatearNumero8Digitos(long numero) {
        return String.format("%08d", Math.max(0L, numero));
    }

    public static String formatearNumeroComprobante(Integer puntoVenta, Long numero) {
        if (numero == null) {
            return null;
        }
        return formatearPuntoVenta(puntoVenta) + "-" + formatearNumero8Digitos(numero);
    }
}
