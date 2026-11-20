import {
    ArticuloPrecioDTO,
    FacturaVentaDetalleDTO,
    FacturaVentaDetalleRequestDTO
} from './api.dto';
import { calcularIvaDetalle, calcularSubtotalDetalle, DetalleCalculable } from '../utils/calculos-factura';

export interface FacturaDetalleProps {
    articuloId: number;
    listaPrecioArticuloId: number;
    descripcion?: string;
    cantidad?: number;
    porcentajeBonificacion?: number;
    precioUnitario: number;
    listaPrecioDenominacion: string;
    articuloDenominacion: string;
    articuloCodigo: string;
    articuloMarca?: string;
}

/**
 * Objeto de dominio que representa un renglón (ítem) de una Factura de Venta.
 * Por defecto inicializa la cantidad en 1 y encapsula el cálculo de su subtotal.
 */
export class FacturaDetalle implements DetalleCalculable {
    public articuloId: number;
    public listaPrecioArticuloId: number;
    public descripcion?: string;
    public cantidad: number;
    public porcentajeBonificacion: number;
    public precioUnitario: number;
    public listaPrecioDenominacion: string;
    public articuloDenominacion: string;
    public articuloCodigo: string;
    public articuloMarca?: string;

    constructor(props: FacturaDetalleProps) {
        this.articuloId = props.articuloId;
        this.listaPrecioArticuloId = props.listaPrecioArticuloId;
        this.descripcion = props.descripcion;
        this.cantidad = FacturaDetalle.normalizarCantidad(props.cantidad ?? 1);
        this.porcentajeBonificacion = FacturaDetalle.normalizarBonificacion(props.porcentajeBonificacion ?? 0);
        this.precioUnitario = Math.max(0, props.precioUnitario ?? 0);
        this.listaPrecioDenominacion = props.listaPrecioDenominacion;
        this.articuloDenominacion = props.articuloDenominacion;
        this.articuloCodigo = props.articuloCodigo;
        this.articuloMarca = props.articuloMarca;
    }

    /** Crea un FacturaDetalle a partir de un artículo seleccionado del catálogo, con cantidad = 1 por defecto. */
    static desdeArticulo(articulo: ArticuloPrecioDTO, cantidadInicial: number = 1): FacturaDetalle {
        return new FacturaDetalle({
            articuloId: articulo.id,
            listaPrecioArticuloId: articulo.listaPrecioArticuloId,
            descripcion: undefined,
            cantidad: cantidadInicial,
            porcentajeBonificacion: 0,
            precioUnitario: articulo.precioVenta ?? 0,
            listaPrecioDenominacion: articulo.listaPrecioDenominacion,
            articuloDenominacion: articulo.denominacion,
            articuloCodigo: articulo.codigo,
            articuloMarca: articulo.marca
        });
    }

    /** Reconstruye un FacturaDetalle desde el DTO recibido de la API REST. */
    static desdeDTO(dto: FacturaVentaDetalleDTO): FacturaDetalle {
        return new FacturaDetalle({
            articuloId: dto.articuloId || 0,
            listaPrecioArticuloId: dto.listaPrecioArticuloId || 0,
            descripcion: dto.descripcion || undefined,
            cantidad: dto.cantidad,
            porcentajeBonificacion: dto.porcentajeBonificacion || 0,
            precioUnitario: dto.precioUnitario,
            listaPrecioDenominacion: dto.listaPrecio || '',
            articuloDenominacion: dto.denominacionArticulo || '',
            articuloCodigo: dto.codigoArticulo || '',
            articuloMarca: undefined
        });
    }

    setCantidad(cantidad: number): void {
        this.cantidad = FacturaDetalle.normalizarCantidad(cantidad);
    }

    incrementarCantidad(delta: number = 1): number {
        const incremento = Number.isFinite(delta) && delta > 0 ? delta : 1;
        this.cantidad = FacturaDetalle.normalizarCantidad(this.cantidad + incremento);
        return this.cantidad;
    }

    setPorcentajeBonificacion(porcentaje: number): void {
        this.porcentajeBonificacion = FacturaDetalle.normalizarBonificacion(porcentaje);
    }

    calcularSubtotal(): number {
        return calcularSubtotalDetalle(this);
    }

    calcularIva(): number {
        return calcularIvaDetalle(this);
    }

    toRequestDTO(): FacturaVentaDetalleRequestDTO {
        return {
            articuloId: this.articuloId,
            listaPrecioArticuloId: this.listaPrecioArticuloId,
            descripcion: this.descripcion,
            cantidad: this.cantidad,
            porcentajeBonificacion: this.porcentajeBonificacion
        };
    }

    private static normalizarCantidad(valor: number): number {
        return Number.isFinite(valor) && valor >= 1 ? Math.round(valor) : 1;
    }

    private static normalizarBonificacion(valor: number): number {
        if (!Number.isFinite(valor)) return 0;
        return Math.min(100, Math.max(0, valor));
    }
}
