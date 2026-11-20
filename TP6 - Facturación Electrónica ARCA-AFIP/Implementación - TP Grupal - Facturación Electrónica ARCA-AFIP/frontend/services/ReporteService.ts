import { FacturaFiltrosDTO } from '../models/api.dto';
import { API_BASE_URL } from '../config';
import { construirErrorApi } from './api-utils';

/**
 * Servicio cliente para la generación y descarga de reportes PDF y Excel (/api/reportes).
 */
export class ReporteService {
    private readonly baseUrl: string;

    constructor(baseUrl: string = `${API_BASE_URL}/reportes`) {
        this.baseUrl = baseUrl.replace(/\/+$/, '');
    }

    /**
     * Descarga el reporte consolidado de facturas en formato PDF o Excel aplicando los filtros activos.
     * Genera un Blob desde el endpoint REST y activa la descarga automática en el navegador.
     */
    async descargarReporte(tipo: 'pdf' | 'excel', filtros?: FacturaFiltrosDTO): Promise<void> {
        const extension = tipo === 'excel' ? 'xlsx' : 'pdf';
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
        }
        const query = params.toString();
        const url = query ? `${this.baseUrl}/facturas/${tipo}?${query}` : `${this.baseUrl}/facturas/${tipo}`;
        const response = await fetch(url);
        if (!response.ok) {
            throw await construirErrorApi(response, `Error al generar el reporte ${tipo.toUpperCase()}`);
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
}
