import {
    ArticuloPrecioDTO,
    ClienteDTO,
    CondicionIvaDTO,
    EstadoFactura,
    FacturaFiltrosDTO,
    FacturaVentaDTO,
    FacturaVentaListadoDTO,
    FacturaVentaRequestDTO,
    InformarArcaResultadoDTO,
    ListaPrecioArticuloDTO,
    ListaPrecioDTO,
    PuntoVentaDTO,
    RespuestaPaginadaDTO,
    TipoMonedaDTO
} from '../models/api.dto';

/** Error de la API con el status HTTP disponible (para distinguir 409, 400, etc.). */
export class ApiError extends Error {
    constructor(public readonly status: number, mensaje: string) {
        super(mensaje);
        this.name = 'ApiError';
    }
}

/**
 * Servicio API que centraliza las llamadas HTTP al backend Spring Boot.
 * Consume los endpoints REST (GET, POST, PUT, DELETE) con fetch + async/await.
 */
export class FacturaService {
    private readonly baseUrl: string;
    private readonly apiBase: string;

    constructor(baseUrl: string = 'http://localhost:8080/api/facturas') {
        this.baseUrl = baseUrl;
        this.apiBase = baseUrl.replace(/\/facturas\/?$/, '');
    }

    // ─── Facturas ───────────────────────────────────────────────────────────

    /** GET /api/facturas — listado paginado con filtros. */
    async obtenerFacturas(filtros?: FacturaFiltrosDTO): Promise<RespuestaPaginadaDTO<FacturaVentaListadoDTO>> {
        const params = new URLSearchParams();
        if (filtros) {
            if (filtros.fechaDesde && filtros.fechaDesde.trim() !== '') params.append('fechaDesde', filtros.fechaDesde.trim());
            if (filtros.fechaHasta && filtros.fechaHasta.trim() !== '') params.append('fechaHasta', filtros.fechaHasta.trim());
            if (filtros.estado && filtros.estado.trim() !== '') params.append('estado', filtros.estado.trim());
            if (filtros.montoMinimo !== undefined && filtros.montoMinimo !== null && !isNaN(Number(filtros.montoMinimo))) {
                params.append('montoMinimo', String(filtros.montoMinimo));
            }
            if (filtros.clienteId !== undefined && filtros.clienteId !== null && !isNaN(Number(filtros.clienteId))) {
                params.append('clienteId', String(filtros.clienteId));
            }
            if (filtros.busqueda && filtros.busqueda.trim() !== '') params.append('busqueda', filtros.busqueda.trim());
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
        if (!response.ok) throw await this.errorDe(response, 'Error al obtener facturas');
        return await response.json();
    }

    /** GET /api/facturas/{id} — detalle completo. */
    async obtenerDetalle(id: number): Promise<FacturaVentaDTO> {
        const response = await fetch(`${this.baseUrl}/${id}`, { headers: { Accept: 'application/json' } });
        if (!response.ok) throw await this.errorDe(response, 'Error al obtener la factura');
        return await response.json();
    }

    /** POST /api/facturas — crea en BORRADOR (201). */
    async crearFactura(factura: FacturaVentaRequestDTO): Promise<FacturaVentaDTO> {
        const response = await fetch(this.baseUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(factura)
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al crear la factura');
        return await response.json();
    }

    /** PUT /api/facturas/{id} — edita (409 si el estado no lo permite). */
    async actualizarFactura(id: number, factura: FacturaVentaRequestDTO): Promise<FacturaVentaDTO> {
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(factura)
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al guardar la factura');
        return await response.json();
    }

    /** DELETE /api/facturas/{id} — baja lógica (204). */
    async eliminarFactura(id: number): Promise<void> {
        const response = await fetch(`${this.baseUrl}/${id}`, { method: 'DELETE' });
        if (!response.ok) throw await this.errorDe(response, 'Error al eliminar la factura');
    }

    /** POST /api/facturas/{id}/anular. */
    async anularFactura(id: number): Promise<FacturaVentaDTO> {
        const response = await fetch(`${this.baseUrl}/${id}/anular`, {
            method: 'POST',
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al anular la factura');
        return await response.json();
    }

    /** POST /api/facturas/{id}/emitir · emite factura existente con ARCA/AFIP (obtiene CAE). */
    async emitirFacturaExistente(id: number): Promise<FacturaVentaDTO> {
        const response = await fetch(`${this.baseUrl}/${id}/emitir`, {
            method: 'POST',
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al emitir factura en ARCA');
        return await response.json();
    }

    /** POST /api/facturas/emitir · emite nueva factura con ARCA/AFIP (201 Created). */
    async emitirFactura(factura: FacturaVentaRequestDTO): Promise<FacturaVentaDTO> {
        const response = await fetch(`${this.baseUrl}/emitir`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(factura)
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al emitir factura en ARCA');
        return await response.json();
    }

    /** Emite facturas en lote ante ARCA llamando al endpoint correspondiente para cada una. */
    async informarArca(ids: number[]): Promise<InformarArcaResultadoDTO[]> {
        const resultados: InformarArcaResultadoDTO[] = [];
        for (const id of ids) {
            try {
                const f = await this.emitirFacturaExistente(id);
                resultados.push({
                    id: f.id,
                    nroFactura: f.numero,
                    estado: f.estado,
                    cae: f.cae,
                    caeFechaVencimiento: f.caeFechaVencimiento,
                    fechaEmision: f.fechaEmision,
                    motivoRechazo: f.motivoRechazo,
                    mensaje: f.cae ? `Autorizada por ARCA. CAE: ${f.cae}` : 'Emitida'
                });
            } catch (err) {
                resultados.push({
                    id: id,
                    nroFactura: null,
                    estado: 'RECHAZADA' as EstadoFactura,
                    cae: null,
                    caeFechaVencimiento: null,
                    fechaEmision: null,
                    motivoRechazo: err instanceof Error ? err.message : String(err),
                    mensaje: err instanceof Error ? err.message : 'Error al comunicar con ARCA'
                });
            }
        }
        return resultados;
    }

    /** POST /api/facturas/{id}/informar-arca — informa una sola. */
    async informarArcaUna(id: number): Promise<InformarArcaResultadoDTO[]> {
        const response = await fetch(`${this.baseUrl}/${id}/informar-arca`, {
            method: 'POST',
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al informar a ARCA');
        return await response.json();
    }

    // ─── Artículos ──────────────────────────────────────────────────────────

    /** GET /api/articulos?search=&listaPrecioId= — mínimo 3 caracteres. */
    async buscarArticulos(q: string, listaPrecioId: number): Promise<ArticuloPrecioDTO[]> {
        const params = new URLSearchParams({ search: q, listaPrecioId: String(listaPrecioId) });
        const response = await fetch(`${this.apiBase}/articulos?${params.toString()}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al buscar artículos');
        return await response.json();
    }

    /** GET /api/articulos/{id}/listas-precio. */
    async listasPrecioDeArticulo(articuloId: number): Promise<ListaPrecioArticuloDTO[]> {
        const response = await fetch(`${this.apiBase}/articulos/${articuloId}/listas-precio`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al obtener las listas de precios');
        return await response.json();
    }

    // ─── Catálogos ──────────────────────────────────────────────────────────

    async obtenerListasPrecio(): Promise<ListaPrecioDTO[]> {
        return this.getCatalogo<ListaPrecioDTO>('listas-precio');
    }

    async obtenerPuntosVenta(): Promise<PuntoVentaDTO[]> {
        return this.getCatalogo<PuntoVentaDTO>('puntos-venta');
    }

    async obtenerCondicionesIva(): Promise<CondicionIvaDTO[]> {
        return this.getCatalogo<CondicionIvaDTO>('condiciones-iva');
    }

    async obtenerTiposMoneda(): Promise<TipoMonedaDTO[]> {
        return this.getCatalogo<TipoMonedaDTO>('tipos-moneda');
    }

    private async getCatalogo<T>(ruta: string): Promise<T[]> {
        const response = await fetch(`${this.apiBase}/${ruta}`, { headers: { Accept: 'application/json' } });
        if (!response.ok) throw await this.errorDe(response, `Error al obtener ${ruta}`);
        return await response.json();
    }

    // ─── Clientes ───────────────────────────────────────────────────────────

    /** GET /api/clientes/buscar?filtro=&page=&size= — búsqueda paginada de clientes. */
    async buscarClientes(filtro: string, pagina: number = 0, tamanio: number = 5): Promise<RespuestaPaginadaDTO<ClienteDTO>> {
        const params = new URLSearchParams({
            filtro: filtro ?? '',
            page: String(pagina),
            size: String(tamanio)
        });
        const response = await fetch(`${this.apiBase}/clientes/buscar?${params.toString()}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await this.errorDe(response, 'Error al buscar clientes');
        return await response.json();
    }

    // ─── Reportes ───────────────────────────────────────────────────────────

    /**
     * Descarga el reporte consolidado de facturas en formato PDF o Excel.
     * Genera un Blob desde el endpoint REST y activa la descarga automática en el navegador.
     */
    async descargarReporte(tipo: 'pdf' | 'excel'): Promise<void> {
        const extension = tipo === 'excel' ? 'xlsx' : 'pdf';
        const url = `${this.apiBase}/reportes/facturas/${tipo}`;
        const response = await fetch(url);
        if (!response.ok) {
            throw await this.errorDe(response, `Error al generar el reporte ${tipo.toUpperCase()}`);
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = downloadUrl;

        const disposition = response.headers.get('Content-Disposition');
        let filename = `reporte_facturas.${extension}`;
        if (disposition && disposition.includes('filename=')) {
            const match = disposition.match(/filename="?([^"]+)"?/);
            if (match?.[1]) filename = match[1];
        }

        enlace.download = filename;
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
        setTimeout(() => window.URL.revokeObjectURL(downloadUrl), 1000);
    }

    // ─── Errores ────────────────────────────────────────────────────────────

    /** Arma un ApiError leyendo el `{ mensaje }` del backend si existe. */
    private async errorDe(response: Response, contexto: string): Promise<ApiError> {
        let detalle = response.statusText;
        try {
            const cuerpo = await response.json();
            if (cuerpo && typeof cuerpo.mensaje === 'string') {
                detalle = cuerpo.mensaje;
            } else if (cuerpo && typeof cuerpo.message === 'string') {
                detalle = cuerpo.message;
            } else if (cuerpo && typeof cuerpo.motivoRechazo === 'string') {
                detalle = cuerpo.motivoRechazo;
            }
        } catch {
            // La respuesta no era JSON: nos quedamos con el statusText.
        }
        return new ApiError(response.status, `${contexto} (${response.status}): ${detalle}`);
    }
}
