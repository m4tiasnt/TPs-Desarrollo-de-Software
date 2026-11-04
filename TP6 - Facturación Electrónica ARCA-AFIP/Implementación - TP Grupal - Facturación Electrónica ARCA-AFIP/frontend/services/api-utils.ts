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

/** Construye un ApiError leyendo el `{ mensaje }` del backend si existe. */
export async function construirErrorApi(response: Response, contexto: string): Promise<ApiError> {
    let detalle = response.statusText;
    try {
        const cuerpo = await response.json();
        if (cuerpo && typeof cuerpo.mensaje === 'string') {
            detalle = cuerpo.mensaje;
        }
    } catch {
        // La respuesta no era JSON: nos quedamos con el statusText.
    }
    return new ApiError(response.status, `${contexto} (${response.status}): ${detalle}`);
}
