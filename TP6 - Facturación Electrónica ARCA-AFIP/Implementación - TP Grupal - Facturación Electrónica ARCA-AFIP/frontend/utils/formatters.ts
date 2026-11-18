import { EstadoFactura } from '../models/api.dto';

/**
 * Formatea un número como moneda en Pesos Argentinos ($ 1.250,00).
 */
export function formatearMoneda(valor: number): string {
    const num = isNaN(valor) ? 0 : valor;
    return '$ ' + num.toLocaleString('es-AR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

/**
 * Formatea una fecha ISO (YYYY-MM-DD o ISO string) a formato latinoamericano (DD/MM/AAAA).
 */
export function formatearFecha(valor: string | null | undefined): string {
    if (!valor) return '—';
    const str = String(valor);
    const match = str.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (match) {
        return `${match[3]}/${match[2]}/${match[1]}`;
    }
    const d = new Date(str);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('es-AR');
}

/**
 * Devuelve el número formal de la factura o "Borrador #ID" si todavía no tiene número asignado.
 */
export function obtenerIdentificadorFactura(factura: { id: number; numeroComprobante?: string | null }): string {
    return factura.numeroComprobante || `Borrador #${factura.id}`;
}

/**
 * Configuración visual de cada estado de factura: clase CSS y etiqueta en español.
 */
export const ESTADOS_FACTURA: Record<EstadoFactura, { clase: string; label: string }> = {
    BORRADOR: { clase: 'badge-borrador', label: 'Borrador' },
    EN_PROCESO: { clase: 'badge-en-proceso', label: 'En proceso' },
    AUTORIZADA: { clase: 'badge-autorizada', label: 'Autorizada' },
    RECHAZADA: { clase: 'badge-rechazada', label: 'Rechazada' },
    ANULADA: { clase: 'badge-anulada', label: 'Anulada' }
};

/**
 * Aplica estilos de badge y etiqueta según el estado de la factura.
 * Si está EN_PROCESO, añade un spinner animado.
 */
export function aplicarBadgeEstado(elemento: HTMLElement, estado: EstadoFactura): void {
    const info = ESTADOS_FACTURA[estado] ?? { clase: 'badge-borrador', label: estado };
    elemento.className = `badge ${info.clase}`;
    if (estado === 'EN_PROCESO') {
        elemento.innerHTML = '<span class="spinner" aria-hidden="true"></span>' + info.label;
    } else {
        elemento.textContent = info.label;
    }
}

/**
 * Determina si una factura puede ser informada a ARCA (sólo Borrador o Rechazada).
 */
export function esFacturaInformable(estado: EstadoFactura): boolean {
    return estado === 'BORRADOR' || estado === 'RECHAZADA';
}
