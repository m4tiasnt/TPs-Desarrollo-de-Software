/**
 * Interfaz mínima necesaria para calcular importes de un detalle de factura.
 */
export interface DetalleCalculable {
    cantidad: number;
    precioUnitario: number;
    porcentajeBonificacion?: number;
}

/**
 * Redondea un número a 2 decimales estándar.
 */
export function redondear(valor: number): number {
    return Math.round((valor + Number.EPSILON) * 100) / 100;
}

/**
 * Calcula el subtotal con bonificación de un detalle individual.
 * Fórmula: cantidad * precioUnitario * (1 - bonif / 100)
 */
export function calcularSubtotalDetalle(detalle: DetalleCalculable): number {
    const cantidad = Math.max(0, detalle.cantidad || 0);
    const precio = Math.max(0, detalle.precioUnitario || 0);
    const bonif = Math.min(100, Math.max(0, detalle.porcentajeBonificacion || 0));

    const bruto = cantidad * precio * (1 - bonif / 100);
    return redondear(bruto);
}

/**
 * Totales globales de una factura.
 */
export interface TotalesFactura {
    neto: number;
    iva: number;
    total: number;
}

/**
 * Calcula los totales agregados de la factura: neto gravado, IVA y total.
 * En la versión actual de la API se asume alícuota estándar 21% para el desglose.
 */
export function calcularTotalesFactura(detalles: DetalleCalculable[]): TotalesFactura {
    let acumuladorNeto = 0;
    let acumuladorIva = 0;
    let acumuladorTotal = 0;

    for (const d of detalles) {
        const subtotal = calcularSubtotalDetalle(d);
        const netoDetalle = redondear(subtotal / 1.21);
        const ivaDetalle = subtotal - netoDetalle;

        acumuladorNeto += netoDetalle;
        acumuladorIva += ivaDetalle;
        acumuladorTotal += subtotal;
    }

    return {
        neto: redondear(acumuladorNeto),
        iva: redondear(acumuladorIva),
        total: redondear(acumuladorTotal)
    };
}
