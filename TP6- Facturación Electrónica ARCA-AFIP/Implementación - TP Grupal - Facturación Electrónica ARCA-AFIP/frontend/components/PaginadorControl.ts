import { RespuestaPaginadaDTO } from '../models/api.dto';

export interface PaginadorOptions {
    infoEl: HTMLElement | null;
    prevBtn: HTMLButtonElement | null;
    nextBtn: HTMLButtonElement | null;
    tamanoSelect?: HTMLSelectElement | null;
    etiquetaItem?: string;
    onCambiarPagina: (nuevaPagina: number) => void;
    onCambiarTamano?: (nuevoTamano: number) => void;
}

/**
 * Componente de paginación desacoplado y con layout estable.
 * Calcula rangos numéricos con elipsis (...), desactiva controles durante cargas
 * y mantiene las dimensiones fijas para evitar saltos visuales.
 */
export class PaginadorControl {
    private paginaActual = 0;
    private tamanioPagina = 20;
    private totalPaginas = 0;
    private totalElementos = 0;

    constructor(private readonly options: PaginadorOptions) {
        this.initEvents();
    }

    private initEvents(): void {
        this.options.prevBtn?.addEventListener('click', () => {
            if (this.paginaActual > 0) {
                this.options.onCambiarPagina(this.paginaActual - 1);
            }
        });

        this.options.nextBtn?.addEventListener('click', () => {
            if (this.paginaActual < this.totalPaginas - 1) {
                this.options.onCambiarPagina(this.paginaActual + 1);
            }
        });

        this.options.tamanoSelect?.addEventListener('change', () => {
            const nuevo = parseInt(this.options.tamanoSelect?.value || '20', 10);
            this.tamanioPagina = nuevo;
            this.options.onCambiarTamano?.(nuevo);
        });
    }

    /**
     * Actualiza la barra con la respuesta paginada del backend.
     */
    actualizar<T>(respuesta: RespuestaPaginadaDTO<T>): void {
        this.paginaActual = respuesta.paginaActual;
        this.tamanioPagina = respuesta.tamanioPagina;
        this.totalPaginas = respuesta.totalPaginas;
        this.totalElementos = respuesta.totalElementos;

        this.renderInfo();
        this.renderBotones();
    }

    /**
     * Limpia los botones en caso de 0 resultados o error.
     */
    limpiar(mensajeInfo: string = ''): void {
        this.totalElementos = 0;
        this.totalPaginas = 0;
        if (this.options.infoEl) {
            this.options.infoEl.textContent = mensajeInfo;
        }
        this.removerBotonesNumericos();
        if (this.options.prevBtn) this.options.prevBtn.disabled = true;
        if (this.options.nextBtn) this.options.nextBtn.disabled = true;
    }

    /**
     * Bloquea temporalmente los controles mientras una petición HTTP está en vuelo.
     */
    bloquear(bloqueado: boolean): void {
        if (this.options.prevBtn) {
            this.options.prevBtn.disabled = bloqueado || this.paginaActual <= 0;
        }
        if (this.options.nextBtn) {
            this.options.nextBtn.disabled = bloqueado || this.paginaActual >= this.totalPaginas - 1;
        }

        const nav = this.options.prevBtn?.parentElement;
        if (nav) {
            nav.querySelectorAll<HTMLButtonElement>('.page-btn[data-pagina]').forEach((btn) => {
                btn.disabled = bloqueado;
            });
        }
    }

    private renderInfo(): void {
        if (!this.options.infoEl) return;
        const etiqueta = this.options.etiquetaItem || 'facturas';
        if (this.totalElementos === 0) {
            this.options.infoEl.textContent = '';
            return;
        }

        const desde = this.paginaActual * this.tamanioPagina + 1;
        const hasta = Math.min((this.paginaActual + 1) * this.tamanioPagina, this.totalElementos);
        this.options.infoEl.innerHTML = `Mostrando <strong>${desde}–${hasta}</strong> de <strong>${this.totalElementos}</strong> ${etiqueta}`;
    }

    private removerBotonesNumericos(): void {
        const nav = this.options.prevBtn?.parentElement;
        if (!nav) return;
        nav.querySelectorAll('[data-pagina], .page-ellipsis').forEach((el) => el.remove());
    }

    private renderBotones(): void {
        if (!this.options.prevBtn || !this.options.nextBtn) return;
        const nav = this.options.prevBtn.parentElement;
        if (!nav) return;

        this.removerBotonesNumericos();

        const fragment = document.createDocumentFragment();
        const paginas = this.calcularRangoPaginas(this.paginaActual, this.totalPaginas);

        for (const p of paginas) {
            if (p === -1) {
                const span = document.createElement('span');
                span.className = 'page-ellipsis';
                span.textContent = '…';
                fragment.appendChild(span);
            } else {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'page-btn';
                btn.dataset.pagina = String(p);
                btn.textContent = String(p + 1);
                if (p === this.paginaActual) {
                    btn.setAttribute('aria-current', 'page');
                }
                btn.addEventListener('click', () => {
                    if (p !== this.paginaActual) {
                        this.options.onCambiarPagina(p);
                    }
                });
                fragment.appendChild(btn);
            }
        }

        nav.insertBefore(fragment, this.options.nextBtn);

        this.options.prevBtn.disabled = this.paginaActual <= 0;
        this.options.nextBtn.disabled = this.paginaActual >= this.totalPaginas - 1;
    }

    private calcularRangoPaginas(actual: number, total: number): number[] {
        if (total <= 7) {
            return Array.from({ length: total }, (_, i) => i);
        }
        const paginas: number[] = [0];
        const inicio = Math.max(1, actual - 1);
        const fin = Math.min(total - 2, actual + 1);

        if (inicio > 1) paginas.push(-1);
        for (let i = inicio; i <= fin; i++) {
            paginas.push(i);
        }
        if (fin < total - 2) paginas.push(-1);
        paginas.push(total - 1);
        return paginas;
    }
}
