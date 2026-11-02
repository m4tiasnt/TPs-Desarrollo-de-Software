/**
 * Componente para mostrar avisos y mensajes de feedback (éxito, error, info)
 * con auto-ocultado y animaciones suaves.
 */
export class NotificadorToast {
    private readonly contenedor: HTMLElement | null;
    private timerOcultar?: number;

    constructor(contenedorId: string = 'mensaje-feedback') {
        this.contenedor = document.getElementById(contenedorId);
    }

    /**
     * Muestra un mensaje con el tipo especificado ('exito' | 'error' | 'info')
     * durante la duración indicada (por defecto 5000ms).
     */
    mostrar(texto: string, tipo: 'exito' | 'error' | 'info' = 'info', duracionMs: number = 5000): void {
        if (!this.contenedor) return;

        if (this.timerOcultar !== undefined) {
            window.clearTimeout(this.timerOcultar);
        }

        this.contenedor.textContent = texto;
        this.contenedor.className = `feedback-mensaje ${tipo}`;
        this.contenedor.style.display = 'block';

        this.timerOcultar = window.setTimeout(() => {
            this.ocultar();
        }, duracionMs);
    }

    ocultar(): void {
        if (!this.contenedor) return;
        this.contenedor.style.display = 'none';
        this.contenedor.textContent = '';
        this.contenedor.className = '';
    }

    exito(texto: string, duracionMs?: number): void {
        this.mostrar(texto, 'exito', duracionMs);
    }

    error(texto: string, duracionMs?: number): void {
        this.mostrar(texto, 'error', duracionMs);
    }

    info(texto: string, duracionMs?: number): void {
        this.mostrar(texto, 'info', duracionMs);
    }
}

