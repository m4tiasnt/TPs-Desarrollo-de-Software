import { ClienteDTO, RespuestaPaginadaDTO } from '../models/api.dto';
import { construirErrorApi } from './api-utils';

/**
 * Servicio cliente para el dominio de Clientes (/api/clientes).
 */
export class ClienteService {
    private readonly baseUrl: string;

    constructor(baseUrl: string = 'http://localhost:8080/api/clientes') {
        this.baseUrl = baseUrl;
    }

    /** GET /api/clientes/buscar?filtro=&page=&size= — búsqueda paginada de clientes. */
    async buscarClientes(filtro: string, pagina: number = 0, tamanio: number = 5): Promise<RespuestaPaginadaDTO<ClienteDTO>> {
        const params = new URLSearchParams({
            filtro: filtro ?? '',
            page: String(pagina),
            size: String(tamanio)
        });
        const response = await fetch(`${this.baseUrl}/buscar?${params.toString()}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await construirErrorApi(response, 'Error al buscar clientes');
        return await response.json();
    }
}
