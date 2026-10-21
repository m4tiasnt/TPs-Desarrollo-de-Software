/**
 * DTOs de Facturación (espejo de los DTOs del backend)
 */

// 1. DTO de reporte/salida (FacturaReporteDTO.java)
export interface FacturaReporteDTO {
    id: number;
    numeroFactura: number;
    fechaEmision: string;
    clienteDenominacion: string;
    condicionIva: string;
    puntoVentaDescripcion: string;
    importeTotal: number;
    cantidadItems: number;
    estado: string;
}

// 2. DTO de renglón/detalle de entrada (FacturaDetalleRequestDTO.java)
export interface FacturaDetalleRequestDTO {
    articuloId?: number;
    listaPrecioArticuloId?: number;
    descripcion?: string;
    cantidad: number;
    porcentajeBonificacion?: number;
}

// 3. DTO de cabecera de entrada (FacturaRequestDTO.java)
export interface FacturaRequestDTO {
    numero?: number;
    fechaEmision?: string;
    clienteId?: number;
    condicionIvaId?: number;
    puntoVentaId?: number;
    tipoMonedaId?: number;
    estado?: string;
    observaciones?: string;
    detalles?: FacturaDetalleRequestDTO[];
}

// 4. Precio de un artículo en una lista de precios (ListaPrecioArticuloDTO.java)
export interface ListaPrecioArticuloDTO {
    id: number;
    listaPrecioId: number;
    listaPrecio: string;
    precioVenta: number;
}

// 5. DTO de filtros para búsquedas (parámetros de FacturaRestController)
export interface FacturaFiltrosDTO {
    fechaDesde?: string;
    fechaHasta?: string;
    estado?: string;
    montoMinimo?: number;
}

// 6. Catálogos para selects del formulario de Nueva Factura
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

// 7. Artículo para combobox de búsqueda
export interface ArticuloSearchDTO {
    id: number;
    codigo: string;
    denominacion: string;
    marca: string;
    precioVenta: number;
    listaPrecioArticuloId: number;
}
