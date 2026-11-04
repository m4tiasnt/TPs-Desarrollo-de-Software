import { ArticuloPrecioDTO, ListaPrecioArticuloDTO } from '../models/api.dto';
import { construirErrorApi } from './api-utils';

/**
 * Servicio cliente para el dominio de Artículos (/api/articulos).
 */
export class ArticuloService {
    private readonly baseUrl: string;

    constructor(baseUrl: string = 'http://localhost:8080/api/articulos') {
        this.baseUrl = baseUrl;
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
