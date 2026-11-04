import { ArticuloPrecioDTO } from '../models/api.dto';
import { ArticuloService } from '../services/ArticuloService';
import { formatearMoneda } from '../utils/formatters';
import { setTexto } from '../utils/dom';

export interface BuscadorArticuloOptions {
    inputEl: HTMLInputElement;
    listaEl: HTMLUListElement;
    tplOpcion: HTMLTemplateElement;
    articuloService: ArticuloService;
    getListaPrecioId: () => number | null;
    onSeleccionarArticulo: (articulo: ArticuloPrecioDTO) => void;
    onError?: (mensaje: string) => void;
}

/**
 * Componente de búsqueda interactiva (combobox) de artículos.
 * Implementa accesibilidad WAI-ARIA (role="combobox", role="listbox"),
 * navegación por flechas de teclado, debounce de búsqueda y selección con Enter/Click.
 */
export class BuscadorArticuloCombobox {
    private indiceActivo = -1;
    private pendienteMarcarPrimera = false;
    private debounceTimer?: number;

    constructor(private readonly options: BuscadorArticuloOptions) {
        this.initEvents();
    }

    private initEvents(): void {
        this.options.inputEl.addEventListener('input', () => {
            if (this.debounceTimer !== undefined) {
                window.clearTimeout(this.debounceTimer);
            }
            this.debounceTimer = window.setTimeout(() => {
                void this.buscar(this.options.inputEl.value.trim());
            }, 250);
        });

        this.options.inputEl.addEventListener('keydown', (e) => this.manejarTeclado(e));

        // Cerrar lista al hacer clic fuera del combobox
        document.addEventListener('click', (e) => {
            const dentro = this.options.inputEl.contains(e.target as Node) || this.options.listaEl.contains(e.target as Node);
            if (!dentro) {
                this.cerrar();
            }
        });
    }

    cerrar(): void {
        this.options.listaEl.hidden = true;
        this.options.inputEl.setAttribute('aria-expanded', 'false');
        this.options.inputEl.removeAttribute('aria-activedescendant');
        this.indiceActivo = -1;
        this.pendienteMarcarPrimera = false;
    }

    private opciones(): HTMLLIElement[] {
        return Array.from(this.options.listaEl.querySelectorAll<HTMLLIElement>('li[role="option"]'));
    }

    private marcarOpcionActiva(indice: number): void {
        const ops = this.opciones();
        if (ops.length === 0) {
            this.indiceActivo = -1;
            return;
        }
        this.indiceActivo = indice;
        ops.forEach((li, i) => li.setAttribute('aria-selected', String(i === indice)));
        const activa = ops[indice];
        this.options.inputEl.setAttribute('aria-activedescendant', activa.id);
        activa.scrollIntoView({ block: 'nearest' });
    }

    async buscar(termino: string): Promise<void> {
        const listaBaseId = this.options.getListaPrecioId();
        if (termino.length < 3 || listaBaseId == null) {
            this.cerrar();
            return;
        }

        try {
            const todos = await this.options.articuloService.buscarArticulos(termino, listaBaseId);
            const resultados = todos.slice(0, 10);
            this.options.listaEl.replaceChildren();
            this.indiceActivo = -1;
            this.options.inputEl.removeAttribute('aria-activedescendant');

            if (resultados.length === 0) {
                const li = document.createElement('li');
                li.className = 'combobox-empty-item';
                li.textContent = 'Sin resultados';
                this.options.listaEl.appendChild(li);
            } else {
                const fragment = document.createDocumentFragment();
                for (const art of resultados) {
                    const clone = this.options.tplOpcion.content.cloneNode(true) as DocumentFragment;
                    const li = clone.querySelector('li') as HTMLLIElement;
                    li.id = `opcion-articulo-${art.id}`;
                    li.setAttribute('aria-selected', 'false');

                    setTexto(li, '[data-campo="denominacion"]', art.denominacion);
                    setTexto(li, '[data-campo="detalle"]', [art.codigo, art.marca].filter(Boolean).join(' · '));

                    const precioEl = li.querySelector('[data-campo="precio"]') as HTMLElement;
                    if (art.precioVenta && art.precioVenta > 0) {
                        precioEl.textContent = formatearMoneda(art.precioVenta);
                    } else {
                        precioEl.innerHTML = '<span class="badge badge-pendiente">Sin precio</span>';
                    }

                    li.addEventListener('click', () => {
                        this.options.onSeleccionarArticulo(art);
                        this.cerrar();
                        this.options.inputEl.value = '';
                    });

                    fragment.appendChild(li);
                }

                if (todos.length > resultados.length) {
                    const hintLi = document.createElement('li');
                    hintLi.className = 'combobox-empty-item';
                    hintLi.style.fontSize = '11px';
                    hintLi.style.borderTop = '1px solid var(--color-border)';
                    hintLi.textContent = `Mostrando los primeros 10 de ${todos.length} resultados. Escriba más caracteres para acotar.`;
                    fragment.appendChild(hintLi);
                }

                this.options.listaEl.appendChild(fragment);
            }

            this.options.listaEl.hidden = false;
            this.options.inputEl.setAttribute('aria-expanded', 'true');

            if (this.pendienteMarcarPrimera && resultados.length > 0) {
                this.pendienteMarcarPrimera = false;
                this.marcarOpcionActiva(0);
            }
        } catch (e) {
            this.pendienteMarcarPrimera = false;
            this.options.onError?.(e instanceof Error ? e.message : 'Error al buscar artículos');
        }
    }

    private manejarTeclado(e: KeyboardEvent): void {
        if (e.key === 'Escape') {
            if (!this.options.listaEl.hidden) {
                e.preventDefault();
                e.stopPropagation();
                this.cerrar();
            }
            return;
        }

        if (e.key === 'Enter') {
            const ops = this.opciones();
            if (!this.options.listaEl.hidden && ops.length > 0) {
                e.preventDefault();
                ops[this.indiceActivo >= 0 ? this.indiceActivo : 0].click();
            }
            return;
        }

        if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;

        e.preventDefault();
        const paso = e.key === 'ArrowDown' ? 1 : -1;

        if (this.options.listaEl.hidden) {
            const consulta = this.options.inputEl.value.trim();
            if (consulta.length >= 3) {
                this.pendienteMarcarPrimera = paso > 0;
                void this.buscar(consulta);
            }
            return;
        }

        const total = this.opciones().length;
        if (total === 0) return;

        let siguiente = this.indiceActivo === -1 ? (paso > 0 ? 0 : total - 1) : this.indiceActivo + paso;
        if (siguiente < 0) siguiente = total - 1;
        if (siguiente >= total) siguiente = 0;
        this.marcarOpcionActiva(siguiente);
    }
}

