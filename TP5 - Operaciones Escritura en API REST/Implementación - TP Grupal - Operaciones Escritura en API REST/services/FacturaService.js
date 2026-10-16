/**
 * Servicio API para centralizar llamadas HTTP al backend Spring Boot.
 * Consume los endpoints REST (GET, POST, PUT, DELETE) usando fetch y async/await.
 */
export class FacturaService {
    constructor(baseUrl = 'http://localhost:8080/api/facturas') {
        this.baseUrl = baseUrl;
    }
    /**
     * Obtiene la lista de facturas aplicando filtros opcionales.
     * GET /api/facturas
     */
    async obtenerFacturas(filtros) {
        let url = this.baseUrl;
        if (filtros) {
            const params = new URLSearchParams();
            if (filtros.fechaDesde)
                params.append('fechaDesde', filtros.fechaDesde);
            if (filtros.fechaHasta)
                params.append('fechaHasta', filtros.fechaHasta);
            if (filtros.estado)
                params.append('estado', filtros.estado);
            if (filtros.montoMinimo !== undefined && filtros.montoMinimo !== null) {
                params.append('montoMinimo', filtros.montoMinimo.toString());
            }
            const queryString = params.toString();
            if (queryString) {
                url += `?${queryString}`;
            }
        }
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });
        if (!response.ok) {
            throw new Error(`Error al obtener facturas (${response.status}): ${response.statusText}`);
        }
        return await response.json();
    }
    /**
     * Crea una nueva factura enviando el DTO de entrada.
     * POST /api/facturas
     */
    async crearFactura(factura) {
        const response = await fetch(this.baseUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(factura)
        });
        if (!response.ok) {
            const errorBody = await response.text();
            throw new Error(`Error al crear la factura (${response.status}): ${errorBody || response.statusText}`);
        }
        return await response.json();
    }
    /**
     * Actualiza el estado y observaciones de una factura existente.
     * PUT /api/facturas/{id} (Retorna 200 OK con FacturaReporteDTO)
     */
    async actualizarEstado(id, estado, observaciones) {
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
            const errorBody = await response.text();
            throw new Error(`Error al actualizar estado (${response.status}): ${errorBody || response.statusText}`);
        }
        return await response.json();
    }
    /**
     * Elimina físicamente una factura por su ID.
     * DELETE /api/facturas/{id}
     */
    async eliminarFactura(id) {
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const errorBody = await response.text();
            throw new Error(`Error al eliminar la factura (${response.status}): ${errorBody || response.statusText}`);
        }
    }
}
