/**
 * DTOs de Facturación (Espejo de Spring Boot DTOs)
 * Correspondencia exacta de nombres de campos y tipos de datos con Java.
 */

// 1. DTO de reporte/salida (Espejo de FacturaReporteDTO.java)
export interface FacturaReporteDTO {
    id?: number;
    numeroFactura: number;
    fechaEmision: string | Date;
    clienteDenominacion: string;
    condicionIva: string;
    puntoVentaDescripcion: string;
    importeTotal: number;
    cantidadItems: number;
    estado?: string;
}

// 2. DTO de renglón/detalle de entrada (Espejo de FacturaDetalleRequestDTO.java)
export interface FacturaDetalleRequestDTO {
    listaPrecioArticuloId?: number;
    descripcion?: string;
    cantidad: number;
    porcentajeBonificacion?: number;
    importeSubtotal?: number;
}

// 3. DTO de cabecera de entrada (Espejo de FacturaRequestDTO.java)
export interface FacturaRequestDTO {
    numero?: number;
    fechaEmision?: string | Date;
    clienteId: number;
    condicionIvaId: number;
    puntoVentaId: number;
    tipoMonedaId: number;
    estado?: string;
    observaciones?: string;
    detalles: FacturaDetalleRequestDTO[];
}

// 4. DTO de filtros para búsquedas (Espejo de parámetros de FacturaRestController)
export interface FacturaFiltrosDTO {
    fechaDesde?: string;
    fechaHasta?: string;
    estado?: string;
    montoMinimo?: number;
}
