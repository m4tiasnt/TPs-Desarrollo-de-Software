import { construirErrorApi } from './api-utils';

/**
 * Servicio cliente para la generación y descarga de reportes PDF y Excel (/api/reportes).
 */
export class ReporteService {
    private readonly baseUrl: string;

    constructor(baseUrl: string = 'http://localhost:8080/api/reportes') {
        this.baseUrl = baseUrl.replace(/\/+$/, '');
    }

    /**
     * Descarga el reporte consolidado de facturas en formato PDF o Excel.
     * Genera un Blob desde el endpoint REST y activa la descarga automática en el navegador.
     */
    async descargarReporte(tipo: 'pdf' | 'excel'): Promise<void> {
        const extension = tipo === 'excel' ? 'xlsx' : 'pdf';
        const url = `${this.baseUrl}/facturas/${tipo}`;
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
