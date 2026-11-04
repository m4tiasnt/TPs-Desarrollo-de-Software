import {
    FacturaFiltrosDTO,
    FacturaVentaDTO,
    FacturaVentaListadoDTO,
    FacturaVentaRequestDTO,
    ResultadoAutorizacionDTO,
    RespuestaPaginadaDTO
} from '../models/api.dto';
import { ApiError, construirErrorApi } from './api-utils';

export { ApiError };

/**
 * Servicio API exclusivo para el dominio de Facturación (/api/facturas).
 * Gestiona el ciclo de vida de facturas (CRUD, anulación) y la comunicación fiscal con ARCA.
 */
export class FacturaService {
    private readonly baseUrl: string;

    constructor(baseUrl: string = 'http://localhost:8080/api/facturas') {
        this.baseUrl = baseUrl.replace(/\/+$/, '');
    }

    // ─── Facturas ───────────────────────────────────────────────────────────

    /** GET /api/facturas — consulta paginada con filtros combinados. */
    async obtenerFacturas(filtros?: FacturaFiltrosDTO): Promise<RespuestaPaginadaDTO<FacturaVentaListadoDTO>> {
        const params = new URLSearchParams();

        if (filtros) {
            if (filtros.fechaDesde) params.append('fechaDesde', filtros.fechaDesde);
            if (filtros.fechaHasta) params.append('fechaHasta', filtros.fechaHasta);
            if (filtros.estado) params.append('estado', filtros.estado);
            if (filtros.montoMinimo !== undefined && filtros.montoMinimo !== null) {
                params.append('montoMinimo', String(filtros.montoMinimo));
            }
            if (filtros.clienteId !== undefined && filtros.clienteId !== null) {
                params.append('clienteId', String(filtros.clienteId));
            }
            if (filtros.busqueda) params.append('busqueda', filtros.busqueda);
            if (filtros.informadaArca !== undefined && filtros.informadaArca !== null) {
                params.append('informadaArca', String(filtros.informadaArca));
            }
            if (filtros.pagina !== undefined && filtros.pagina !== null) {
                params.append('pagina', String(filtros.pagina));
            }
            if (filtros.tamanio !== undefined && filtros.tamanio !== null) {
                params.append('tamanio', String(filtros.tamanio));
            }
        }

        const query = params.toString();
        const url = query ? `${this.baseUrl}?${query}` : this.baseUrl;

        const response = await fetch(url, { headers: { Accept: 'application/json' } });
        if (!response.ok) throw await construirErrorApi(response, 'Error al obtener el listado de facturas');
        return await response.json();
    }

    /** GET /api/facturas/{id} — detalle completo con artículos, cliente y totales. */
    async obtenerDetalle(id: number): Promise<FacturaVentaDTO> {
        const response = await fetch(`${this.baseUrl}/${id}`, { headers: { Accept: 'application/json' } });
        if (!response.ok) throw await construirErrorApi(response, `Error al obtener la factura #${id}`);
        return await response.json();
    }

    /** POST /api/facturas — guarda una factura en estado BORRADOR. */
    async crearFactura(factura: FacturaVentaRequestDTO): Promise<FacturaVentaDTO> {
        const response = await fetch(this.baseUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            body: JSON.stringify(factura)
        });
        if (!response.ok) throw await construirErrorApi(response, 'Error al guardar el borrador de la factura');
        return await response.json();
    }

    /** PUT /api/facturas/{id} — actualiza cabecera y detalles de una factura editable. */
    async actualizarFactura(id: number, factura: FacturaVentaRequestDTO): Promise<FacturaVentaDTO> {
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            body: JSON.stringify(factura)
        });
        if (!response.ok) throw await construirErrorApi(response, `Error al actualizar la factura #${id}`);
        return await response.json();
    }

    /** DELETE /api/facturas/{id} — baja lógica de un borrador. */
    async eliminarFactura(id: number): Promise<void> {
        const response = await fetch(`${this.baseUrl}/${id}`, { method: 'DELETE' });
        if (!response.ok) throw await construirErrorApi(response, `Error al eliminar la factura #${id}`);
    }

    /** POST /api/facturas/{id}/anular — cambia el estado a ANULADA. */
    async anularFactura(id: number): Promise<FacturaVentaDTO> {
        const response = await fetch(`${this.baseUrl}/${id}/anular`, {
            method: 'POST',
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await construirErrorApi(response, `Error al anular la factura #${id}`);
        return await response.json();
    }

    /** POST /api/facturas/{id}/informar-arca — informa una factura individual ante ARCA y obtiene CAE. */
    async informarArca(id: number): Promise<ResultadoAutorizacionDTO> {
        const response = await fetch(`${this.baseUrl}/${id}/informar-arca`, {
            method: 'POST',
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await construirErrorApi(response, 'Error al informar factura a ARCA');
        return await response.json();
    }

    /** POST /api/facturas/informar-arca-lote — informa un lote de facturas ante ARCA y obtiene CAE. */
    async informarArcaLote(ids: number[]): Promise<ResultadoAutorizacionDTO[]> {
        const response = await fetch(`${this.baseUrl}/informar-arca-lote`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            body: JSON.stringify(ids)
        });
        if (!response.ok) throw await construirErrorApi(response, 'Error al informar lote a ARCA');
        return await response.json();
    }
}
