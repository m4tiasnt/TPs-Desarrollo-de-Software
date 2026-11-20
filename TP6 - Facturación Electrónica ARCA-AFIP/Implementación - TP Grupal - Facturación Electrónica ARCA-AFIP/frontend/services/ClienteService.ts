import { ClienteDTO, RespuestaPaginadaDTO } from '../models/api.dto';
import { API_BASE_URL } from '../config';
import { construirErrorApi } from './api-utils';

export interface FiltrosCliente {
    cuit?: string;
    razonSocial?: string;
}

/**
 * Servicio cliente para el dominio de Clientes (/api/clientes).
 */
export class ClienteService {
    private readonly baseUrl: string;

    constructor(baseUrl: string = `${API_BASE_URL}/clientes`) {
        this.baseUrl = baseUrl.replace(/\/+$/, '');
    }

    /** GET /api/clientes/buscar?cuit=&razonSocial=&page=&size= — búsqueda paginada de clientes. */
    async buscarClientes(filtros: FiltrosCliente = {}, pagina: number = 0, tamanio: number = 5): Promise<RespuestaPaginadaDTO<ClienteDTO>> {
        const params = new URLSearchParams({
            cuit: (filtros.cuit ?? '').trim(),
            razonSocial: (filtros.razonSocial ?? '').trim(),
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
