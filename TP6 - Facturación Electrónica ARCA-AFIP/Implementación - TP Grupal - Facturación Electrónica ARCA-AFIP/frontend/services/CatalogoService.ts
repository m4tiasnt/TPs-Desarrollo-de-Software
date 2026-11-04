import {
    CondicionIvaDTO,
    ListaPrecioDTO,
    PuntoVentaDTO,
    TipoMonedaDTO
} from '../models/api.dto';
import { construirErrorApi } from './api-utils';

/**
 * Servicio cliente para los catálogos auxiliares (listas de precios, puntos de venta, IVA, monedas).
 */
export class CatalogoService {
    private readonly apiBase: string;

    constructor(apiBase: string = 'http://localhost:8080/api') {
        this.apiBase = apiBase.replace(/\/+$/, '');
    }

    /** GET /api/listas-precio */
    async obtenerListasPrecio(): Promise<ListaPrecioDTO[]> {
        return this.getCatalogo<ListaPrecioDTO>('listas-precio');
    }

    /** GET /api/puntos-venta */
    async obtenerPuntosVenta(): Promise<PuntoVentaDTO[]> {
        return this.getCatalogo<PuntoVentaDTO>('puntos-venta');
    }

    /** GET /api/condiciones-iva */
    async obtenerCondicionesIva(): Promise<CondicionIvaDTO[]> {
        return this.getCatalogo<CondicionIvaDTO>('condiciones-iva');
    }

    /** GET /api/tipos-moneda */
    async obtenerTiposMoneda(): Promise<TipoMonedaDTO[]> {
        return this.getCatalogo<TipoMonedaDTO>('tipos-moneda');
    }

    private async getCatalogo<T>(ruta: string): Promise<T[]> {
        const response = await fetch(`${this.apiBase}/${ruta}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw await construirErrorApi(response, `Error al obtener catálogo ${ruta}`);
        return await response.json();
    }
}
