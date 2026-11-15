import { ArticuloPrecioDTO, ListaPrecioArticuloDTO, RespuestaPaginadaDTO } from '../models/api.dto';
import { construirErrorApi } from './api-utils';

export interface FiltrosArticulo {
    listaPrecioId: number;
    codigo?: string;
    termino?: string;
}

/**
 * Servicio cliente para el dominio de Artículos (/api/articulos).
 */
export class ArticuloService {
    private readonly baseUrl: string;

    constructor(baseUrl: string = 'http://localhost:8080/api/articulos') {
        this.baseUrl = baseUrl;
    }

    /** GET /api/articulos/buscar?listaPrecioId=&codigo=&termino=&page=&size= — búsqueda paginada. */
    async buscarArticulosPaginados(
        filtros: FiltrosArticulo,
        pagina: number = 0,
        tamanio: number = 5
    ): Promise<RespuestaPaginadaDTO<ArticuloPrecioDTO>> {
        const params = new URLSearchParams({
            listaPrecioId: String(filtros.listaPrecioId),
            codigo: (filtros.codigo ?? '').trim(),
            termino: (filtros.termino ?? '').trim(),
            page: String(pagina),
            size: String(tamanio)
        });
        const response = await fetch(`${this.baseUrl}/buscar?${params.toString()}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await construirErrorApi(response, 'Error al buscar artículos');
        return await response.json();
    }

    /** GET /api/articulos?search=&listaPrecioId= — mínimo 3 caracteres. */
    async buscarArticulos(q: string, listaPrecioId: number): Promise<ArticuloPrecioDTO[]> {
        const params = new URLSearchParams({ search: q, listaPrecioId: String(listaPrecioId) });
        const response = await fetch(`${this.baseUrl}?${params.toString()}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await construirErrorApi(response, 'Error al buscar artículos');
        return await response.json();
    }

    /** GET /api/articulos/{id}/listas-precio — listas de precios disponibles para un artículo. */
    async listasPrecioDeArticulo(articuloId: number): Promise<ListaPrecioArticuloDTO[]> {
        const response = await fetch(`${this.baseUrl}/${articuloId}/listas-precio`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await construirErrorApi(response, 'Error al obtener las listas de precios del artículo');
        return await response.json();
    }
}
