import {
    EstadoFactura,
    FacturaVentaDTO,
    FacturaVentaRequestDTO
} from './api.dto';
import { FacturaDetalle, FacturaDetalleProps } from './FacturaDetalle';
import { calcularTotalesFactura, TotalesFactura } from '../utils/calculos-factura';

export interface ResultadoAgregarDetalle {
    /** true si el artículo ya estaba en la factura y se sumó a su renglón */
    fusionado: boolean;
    indice: number;
    /** cantidad final del renglón */
    cantidad: number;
    detalle: FacturaDetalle;
}

export interface FacturaDatosCabecera {
    clienteId?: number;
    condicionIvaId?: number;
    puntoVentaId?: number;
    tipoMonedaId?: number;
    observaciones?: string;
}

/**
 * Objeto de dominio que representa una Factura de Venta y su colección de FacturaDetalle.
 * Centraliza el agregado sin duplicados por artículo, edición de renglones y cálculo de totales.
 */
export class Factura {
    public id: number | null = null;
    public numero: number | null = null;
    public numeroComprobante: string | null = null;
    public estado: EstadoFactura = 'BORRADOR';

    public clienteId?: number;
    public condicionIvaId?: number;
    public puntoVentaId?: number;
    public tipoMonedaId?: number;
    public observaciones?: string;

    private _detalles: FacturaDetalle[] = [];

    get detalles(): readonly FacturaDetalle[] {
        return this._detalles;
    }

    /** Reconstruye el objeto Factura junto a todos sus objetos FacturaDetalle desde el DTO del backend. */
    static desdeDTO(dto: FacturaVentaDTO): Factura {
        const factura = new Factura();
        factura.id = dto.id;
        factura.numero = dto.numero;
        factura.numeroComprobante = dto.numeroComprobante;
        factura.estado = dto.estado;
        factura.clienteId = dto.clienteId ?? undefined;
        factura.condicionIvaId = dto.condicionIvaId ?? undefined;
        factura.puntoVentaId = dto.puntoVentaId ?? undefined;
        factura.tipoMonedaId = dto.tipoMonedaId ?? undefined;
        factura.observaciones = dto.observaciones ?? undefined;

        const detalles = (dto.detalles || []).map((d) => FacturaDetalle.desdeDTO(d));
        factura.setDetalles(detalles);
        return factura;
    }

    setCabecera(datos: FacturaDatosCabecera): void {
        this.clienteId = datos.clienteId;
        this.condicionIvaId = datos.condicionIvaId;
        this.puntoVentaId = datos.puntoVentaId;
        this.tipoMonedaId = datos.tipoMonedaId;
        this.observaciones = datos.observaciones;
    }

    /**
     * Agrega un FacturaDetalle a la factura.
     * Si ya existe un renglón con el mismo artículo, no agrega uno nuevo: incrementa su cantidad.
     */
    agregarDetalle(detalle: FacturaDetalle): ResultadoAgregarDetalle {
        const indice = detalle.articuloId > 0
            ? this._detalles.findIndex((d) => d.articuloId === detalle.articuloId)
            : -1;

        if (indice === -1) {
            this._detalles.push(detalle);
            return {
                fusionado: false,
                indice: this._detalles.length - 1,
                cantidad: detalle.cantidad,
                detalle
            };
        }

        const existente = this._detalles[indice];
        const cantidadFinal = existente.incrementarCantidad(detalle.cantidad);
        return {
            fusionado: true,
            indice,
            cantidad: cantidadFinal,
            detalle: existente
        };
    }

    setDetalles(detalles: FacturaDetalle[]): void {
        this._detalles = [];
        for (const detalle of detalles) {
            this.agregarDetalle(detalle);
        }
    }

    obtenerDetalles(): FacturaDetalle[] {
        return [...this._detalles];
    }

    quitarDetalle(indice: number): void {
        if (indice >= 0 && indice < this._detalles.length) {
            this._detalles.splice(indice, 1);
        }
    }

    actualizarDetalle(indice: number, cambios: Partial<FacturaDetalleProps>): void {
        const actual = this._detalles[indice];
        if (!actual) return;

        if (cambios.listaPrecioArticuloId !== undefined) {
            actual.listaPrecioArticuloId = cambios.listaPrecioArticuloId;
        }
        if (cambios.precioUnitario !== undefined) {
            actual.precioUnitario = Math.max(0, cambios.precioUnitario);
        }
        if (cambios.listaPrecioDenominacion !== undefined) {
            actual.listaPrecioDenominacion = cambios.listaPrecioDenominacion;
        }
        if ('descripcion' in cambios) {
            actual.descripcion = cambios.descripcion;
        }
        if (cambios.cantidad !== undefined) {
            actual.setCantidad(cambios.cantidad);
        }
        if (cambios.porcentajeBonificacion !== undefined) {
            actual.setPorcentajeBonificacion(cambios.porcentajeBonificacion);
        }
    }

    calcularTotales(): TotalesFactura {
        return calcularTotalesFactura(this._detalles);
    }

    toRequestDTO(): FacturaVentaRequestDTO {
        return {
            clienteId: this.clienteId,
            condicionIvaId: this.condicionIvaId,
            puntoVentaId: this.puntoVentaId,
            tipoMonedaId: this.tipoMonedaId,
            observaciones: this.observaciones,
            detalles: this._detalles.map((d) => d.toRequestDTO())
        };
    }
}
