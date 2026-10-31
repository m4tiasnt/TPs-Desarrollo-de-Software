import {
    FacturaReporteDTO,
    FacturaRequestDTO,
    FacturaFiltrosDTO,
    RespuestaPaginadaDTO,
    ListaPrecioArticuloDTO,
    ListaPrecioDTO,
    ClienteDTO,
    PuntoVentaDTO,
    CondicionIvaDTO,
    TipoMonedaDTO,
    ArticuloSearchDTO
} from '../models/api.dto';

/**
 * Servicio API para centralizar llamadas HTTP al backend Spring Boot.
 * Consume los endpoints REST (GET, POST, PUT, DELETE) usando fetch y async/await.
 */
export class FacturaService {
    private baseUrl: string;
    private articulosUrl: string;

    constructor(baseUrl: string = 'http://localhost:8080/api/facturas') {
        this.baseUrl = baseUrl;
        this.articulosUrl = baseUrl.replace('/facturas', '/articulos');
    }

    /**
     * Obtiene facturas paginadas aplicando filtros opcionales.
     * GET /api/facturas?pagina=0&tamanio=5&...
     */
    async obtenerFacturas(filtros?: FacturaFiltrosDTO): Promise<RespuestaPaginadaDTO<FacturaReporteDTO>> {
        let url = this.baseUrl;

        const params = new URLSearchParams();
        if (filtros) {
            if (filtros.fechaDesde) params.append('fechaDesde', filtros.fechaDesde);
            if (filtros.fechaHasta) params.append('fechaHasta', filtros.fechaHasta);
            if (filtros.estado) params.append('estado', filtros.estado);
            if (filtros.montoMinimo !== undefined && filtros.montoMinimo !== null) {
                params.append('montoMinimo', filtros.montoMinimo.toString());
            }
            if (filtros.pagina !== undefined && filtros.pagina !== null) {
                params.append('pagina', filtros.pagina.toString());
            }
            if (filtros.tamanio !== undefined && filtros.tamanio !== null) {
                params.append('tamanio', filtros.tamanio.toString());
            }
        }

        const queryString = params.toString();
        if (queryString) {
            url += `?${queryString}`;
        }

        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(await this.mensajeDeError(response, 'Error al obtener facturas'));
        }

        return await response.json();
    }

    /**
     * Crea una nueva factura enviando el DTO de entrada.
     * POST /api/facturas
     */
    async crearFactura(factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
        const response = await fetch(this.baseUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(factura)
        });

        if (!response.ok) {
            throw new Error(await this.mensajeDeError(response, 'Error al crear la factura'));
        }

        return await response.json();
    }

    /**
     * Actualiza el estado y observaciones de una factura existente.
     * PUT /api/facturas/{id} (retorna 200 OK con FacturaReporteDTO)
     */
    async actualizarEstado(id: number, estado: string, observaciones?: string): Promise<FacturaReporteDTO> {
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                estado,
                observaciones
            })
        });

        if (!response.ok) {
            throw new Error(await this.mensajeDeError(response, 'Error al actualizar estado'));
        }

        return await response.json();
    }

    /**
     * Listas de precios disponibles para un artículo.
     * GET /api/articulos/{id}/listas-precio
     */
    async listasPrecioDeArticulo(articuloId: number): Promise<ListaPrecioArticuloDTO[]> {
        const response = await fetch(`${this.articulosUrl}/${articuloId}/listas-precio`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(await this.mensajeDeError(response, 'Error al obtener las listas de precios'));
        }

        return await response.json();
    }

    /**
     * Da de baja lógicamente una factura (deja de listarse, pero queda registrada).
     * DELETE /api/facturas/{id} (retorna 204 No Content)
     */
    async eliminarFactura(id: number): Promise<void> {
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(await this.mensajeDeError(response, 'Error al eliminar la factura'));
        }
    }

    // === Catálogos para formulario de alta ===

    /**
     * Todas las listas de precios (para selector global "Lista base").
     * GET /api/listas-precio
     */
    async obtenerListasPrecio(): Promise<ListaPrecioDTO[]> {
        const response = await fetch(`${this.baseUrl.replace('/facturas', '')}/listas-precio`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) throw new Error(await this.mensajeDeError(response, 'Error al obtener listas de precio'));
        return await response.json();
    }

    /**
     * Busca artículos por código, nombre o marca (combobox).
     * GET /api/articulos?search=
     */
    async buscarArticulos(q: string): Promise<ArticuloSearchDTO[]> {
        const url = `${this.articulosUrl}?search=${encodeURIComponent(q)}`;
        const response = await fetch(url, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) throw new Error(await this.mensajeDeError(response, 'Error al buscar artículos'));
        return await response.json();
    }

    /**
     * Clientes para select.
     * GET /api/clientes
     */
    async obtenerClientes(): Promise<ClienteDTO[]> {
        const response = await fetch(`${this.baseUrl.replace('/facturas', '')}/clientes`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) throw new Error(await this.mensajeDeError(response, 'Error al obtener clientes'));
        return await response.json();
    }

    /**
     * Puntos de venta para select.
     * GET /api/puntos-venta
     */
    async obtenerPuntosVenta(): Promise<PuntoVentaDTO[]> {
        const response = await fetch(`${this.baseUrl.replace('/facturas', '')}/puntos-venta`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) throw new Error(await this.mensajeDeError(response, 'Error al obtener puntos de venta'));
        return await response.json();
    }

    /**
     * Condiciones IVA para select.
     * GET /api/condiciones-iva
     */
    async obtenerCondicionesIva(): Promise<CondicionIvaDTO[]> {
        const response = await fetch(`${this.baseUrl.replace('/facturas', '')}/condiciones-iva`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) throw new Error(await this.mensajeDeError(response, 'Error al obtener condiciones IVA'));
        return await response.json();
    }

    /**
     * Tipos de moneda para select.
     * GET /api/tipos-moneda
     */
    async obtenerTiposMoneda(): Promise<TipoMonedaDTO[]> {
        const response = await fetch(`${this.baseUrl.replace('/facturas', '')}/tipos-moneda`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) throw new Error(await this.mensajeDeError(response, 'Error al obtener tipos de moneda'));
        return await response.json();
    }

    /**
     * Extrae el mensaje del JSON de error del backend ({ "mensaje": "..." }).
     * Si la respuesta no trae JSON, usa el statusText.
     */
    private async mensajeDeError(response: Response, contexto: string): Promise<string> {
        let detalle = response.statusText;

        try {
            const cuerpo = await response.json();
            if (cuerpo && typeof cuerpo.mensaje === 'string') {
                detalle = cuerpo.mensaje;
            }
        } catch {
            // La respuesta no era JSON: nos quedamos con el statusText.
        }

        return `${contexto} (${response.status}): ${detalle}`;
    }
}
