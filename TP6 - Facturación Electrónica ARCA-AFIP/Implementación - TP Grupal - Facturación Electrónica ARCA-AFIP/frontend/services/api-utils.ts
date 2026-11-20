/**
 * Utilidades compartidas para clientes API REST del frontend.
 */

/** Error tipado de la API con el status HTTP disponible para distinguir 409, 404, 400, etc. */
export class ApiError extends Error {
    constructor(public readonly status: number, mensaje: string) {
        super(mensaje);
        this.name = 'ApiError';
    }
}

/** Construye un ApiError priorizando el `{ mensaje }` amigable enviado por el backend. */
export async function construirErrorApi(response: Response, contexto: string): Promise<ApiError> {
    try {
        const cuerpo = await response.json();
        if (cuerpo && typeof cuerpo.mensaje === 'string' && cuerpo.mensaje.trim()) {
            return new ApiError(response.status, cuerpo.mensaje.trim());
        }
    } catch {
        // La respuesta no era JSON: usamos el mensaje contextual.
    }
    return new ApiError(response.status, contexto);
}
