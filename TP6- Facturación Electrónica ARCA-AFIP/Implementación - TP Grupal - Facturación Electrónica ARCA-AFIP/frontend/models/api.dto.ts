/**
 * DTOs que consume el frontend (espejo de los DTOs del backend, package com.facturaarca.dto).
 * Los campos de fecha llegan como ISO-8601 (string) o null.
 */

/** Enum de estados de una factura de venta (com.facturaarca.enums.EstadoFactura). */
export type EstadoFactura =
    | 'BORRADOR'
    | 'EN_PROCESO'
    | 'AUTORIZADA'
    | 'RECHAZADA'
    | 'ANULADA';

// ─── Salida / consulta ──────────────────────────────────────────────────────

/** Fila del listado (FacturaVentaListadoDTO). */
export interface FacturaVentaListadoDTO {
    id: number;
    numero: number | null;
    numeroComprobante: string | null;
    fechaEmision: string | null;
    clienteDenominacion: string;
    condicionIva: string;
    puntoVentaNumero: number | null;
    puntoVentaDescripcion: string;
    importeTotal: number;
    cantidadItems: number;
    estado: EstadoFactura;
}

/** Cada detalle de una factura (FacturaVentaDetalleDTO). */
export interface FacturaVentaDetalleDTO {
    id: number;
    articuloId: number | null;
    codigoArticulo: string | null;
    denominacionArticulo: string | null;
    listaPrecioArticuloId: number | null;
    listaPrecio: string | null;
    descripcion: string | null;
    cantidad: number;
    precioUnitario: number;
    porcentajeBonificacion: number;
    importeNeto: number;
    importeIva: number;
    importeSubtotal: number;
}

/** Detalle completo de una factura (FacturaVentaDTO). */
export interface FacturaVentaDTO {
    id: number;
    numero: number | null;
    numeroComprobante: string | null;
    fechaEmision: string | null;

    clienteId: number | null;
    clienteDenominacion: string;
    clienteCuitCuil: string | null;

    condicionIvaId: number | null;
    condicionIva: string;

    puntoVentaId: number | null;
    puntoVentaNumero: number | null;
    puntoVentaDescripcion: string;

    tipoMonedaId: number | null;
    tipoMoneda: string;
    tipoMonedaSimbolo: string;

    importeNeto: number;
    importeIva: number;
    importeTotal: number;
    importeCobrado: number;
    importeSaldo: number;

    estado: EstadoFactura;
    cae: string | null;
    caeFechaVencimiento: string | null;
    resultadoAfip: string | null;
    motivoRechazo: string | null;
    observaciones: string | null;

    fechaAlta: string | null;
    fechaModificacion: string | null;
    fechaAnulacion: string | null;

    detalles: FacturaVentaDetalleDTO[];
}

/** Envoltura paginada en español (RespuestaPaginadaDTO<T>). */
export interface RespuestaPaginadaDTO<T> {
    contenido: T[];
    paginaActual: number;
    tamanioPagina: number;
    totalElementos: number;
    totalPaginas: number;
}

/** Filtros de GET /api/facturas. */
export interface FacturaFiltrosDTO {
    fechaDesde?: string;
    fechaHasta?: string;
    estado?: EstadoFactura;
    montoMinimo?: number;
    clienteId?: number;
    /** N° de factura (`42`) o id precedido por `#` (`#10`). */
    busqueda?: string;
    /** true = solo AUTORIZADA; false = todo lo demás. */
    informadaArca?: boolean;
    /** 0-indexed. */
    pagina?: number;
    tamanio?: number;
}

// ─── Entrada (crear / editar) ───────────────────────────────────────────────

/** Cada detalle para crear/editar (FacturaVentaDetalleRequestDTO). */
export interface FacturaVentaDetalleRequestDTO {
    articuloId?: number;
    listaPrecioArticuloId?: number;
    descripcion?: string;
    cantidad: number;
    porcentajeBonificacion?: number;
}

/** Cabecera y detalles para crear/editar (FacturaVentaRequestDTO). */
export interface FacturaVentaRequestDTO {
    clienteId?: number;
    condicionIvaId?: number;
    puntoVentaId?: number;
    tipoMonedaId?: number;
    observaciones?: string;
    detalles: FacturaVentaDetalleRequestDTO[];
}

// ─── ARCA ───────────────────────────────────────────────────────────────────

/** Resultado de la autorización fiscal ante ARCA (ResultadoAutorizacionDTO). */
export interface ResultadoAutorizacionDTO {
    id: number;
    numero: number | null;
    estado: EstadoFactura;
    cae: string | null;
    caeFechaVencimiento: string | null;
    fechaEmision: string | null;
    motivoRechazo: string | null;
    mensaje: string | null;
}

// ─── Catálogos ──────────────────────────────────────────────────────────────

/** Artículo con el precio de la lista (ArticuloPrecioDTO). */
export interface ArticuloPrecioDTO {
    id: number;
    codigo: string;
    denominacion: string;
    marca: string;
    precioVenta: number | null;
    listaPrecioArticuloId: number;
    listaPrecioId: number;
    listaPrecioDenominacion: string;
}

/** Precio de un artículo en una lista concreta (ListaPrecioArticuloDTO). */
export interface ListaPrecioArticuloDTO {
    id: number;
    listaPrecioId: number;
    listaPrecio: string;
    precioVenta: number;
}

export interface ListaPrecioDTO {
    id: number;
    codigo: string;
    denominacion: string;
}

export interface ClienteDTO {
    id: number;
    cuitCuil: string;
    denominacion: string;
}

export interface PuntoVentaDTO {
    id: number;
    numero: number;
    descripcion: string;
}

export interface CondicionIvaDTO {
    id: number;
    codigoAfip: number;
    denominacion: string;
}

export interface TipoMonedaDTO {
    id: number;
    codigoAfip?: number;
    denominacion: string;
    simbolo: string;
}

