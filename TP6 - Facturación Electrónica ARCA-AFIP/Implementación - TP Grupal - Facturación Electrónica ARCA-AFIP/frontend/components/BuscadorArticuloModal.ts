import { ArticuloPrecioDTO } from '../models/api.dto';
import { ArticuloService } from '../services/ArticuloService';
import { formatearMoneda } from '../utils/formatters';
import { PaginadorControl } from './PaginadorControl';

export interface BuscadorArticuloModalOptions {
    articuloService: ArticuloService;
    getListaPrecioId: () => number | null;
    onSeleccionarArticulo: (articulo: ArticuloPrecioDTO) => void;
    onError?: (mensaje: string) => void;
}

/**
 * Componente modal (Explorer) para buscar y seleccionar artículos con paginación integrada.
 * Replica la experiencia del Explorer de Clientes: filtros por código y descripción/marca,
 * tabla paginada desde el backend y selección directa con un clic.
 */
export class BuscadorArticuloModal {
    private readonly dialog: HTMLDialogElement | null;
    private readonly btnCerrar: HTMLButtonElement | null;
    private readonly inputCodigo: HTMLInputElement | null;
    private readonly inputDenominacion: HTMLInputElement | null;
    private readonly btnBuscar: HTMLButtonElement | null;
    private readonly tbody: HTMLTableSectionElement | null;
    private readonly tableWrap: HTMLElement | null;
    private readonly paginador: PaginadorControl;

    private filtroCodigo = '';
    private filtroTermino = '';

    constructor(private readonly options: BuscadorArticuloModalOptions) {
        this.dialog = document.getElementById('modal-articulo-explorer') as HTMLDialogElement | null;
        this.btnCerrar = document.getElementById('btnCerrarModalArticulo') as HTMLButtonElement | null;
        this.inputCodigo = document.getElementById('inputBuscarArticuloCodigo') as HTMLInputElement | null;
        this.inputDenominacion = document.getElementById('inputBuscarArticuloDenominacion') as HTMLInputElement | null;
        this.btnBuscar = document.getElementById('btnBuscarArticulo') as HTMLButtonElement | null;
        this.tbody = document.getElementById('tbody-articulos-explorer') as HTMLTableSectionElement | null;
        this.tableWrap = this.dialog?.querySelector('.dialog-table') as HTMLElement | null;

        this.paginador = new PaginadorControl({
            infoEl: document.getElementById('infoPaginacionArticulo'),
            prevBtn: document.getElementById('btnPaginacionArticuloAnterior') as HTMLButtonElement | null,
            nextBtn: document.getElementById('btnPaginacionArticuloSiguiente') as HTMLButtonElement | null,
            etiquetaItem: 'artículos',
            onCambiarPagina: (p) => {
                void this.buscar(this.filtroCodigo, this.filtroTermino, p);
            }
        });

        this.initEvents();
    }

    private leerFiltrosActuales(): { codigo: string; termino: string } {
        return {
            codigo: this.inputCodigo?.value.trim() ?? '',
            termino: this.inputDenominacion?.value.trim() ?? ''
        };
    }

    private initEvents(): void {
        this.btnCerrar?.addEventListener('click', () => this.cerrar());

        this.dialog?.addEventListener('click', (e) => {
            if (e.target === this.dialog) {
                this.cerrar();
            }
        });

        this.btnBuscar?.addEventListener('click', () => {
            const { codigo, termino } = this.leerFiltrosActuales();
            void this.buscar(codigo, termino, 0);
        });

        let debounceTimer: number | undefined;
        const inputs = [this.inputCodigo, this.inputDenominacion].filter(
            (el): el is HTMLInputElement => el !== null
        );

        for (const input of inputs) {
            input.addEventListener('input', () => {
                if (debounceTimer !== undefined) window.clearTimeout(debounceTimer);
                debounceTimer = window.setTimeout(() => {
                    const { codigo, termino } = this.leerFiltrosActuales();
                    void this.buscar(codigo, termino, 0);
                }, 250);
            });

            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    if (debounceTimer !== undefined) window.clearTimeout(debounceTimer);
                    const { codigo, termino } = this.leerFiltrosActuales();
                    void this.buscar(codigo, termino, 0);
                }
            });
        }
    }

    abrir(): void {
        const listaPrecioId = this.options.getListaPrecioId();
        if (listaPrecioId == null) {
            this.options.onError?.('Seleccioná una lista de precios antes de buscar artículos.');
            return;
        }
        if (!this.dialog) return;

        this.dialog.showModal();
        if (this.inputCodigo) this.inputCodigo.value = '';
        if (this.inputDenominacion) this.inputDenominacion.value = '';
        this.inputDenominacion?.focus();
        this.mostrarMensajeMinimoCaracteres();
    }

    private mostrarMensajeMinimoCaracteres(): void {
        if (!this.tbody) return;
        this.filtroCodigo = '';
        this.filtroTermino = '';
        this.tableWrap?.classList.add('is-empty');
        this.tbody.innerHTML = `
            <tr class="fila-vacia">
                <td colspan="4" class="modal-empty-cell">
                    <div class="modal-empty-content">
                        <span class="modal-empty-title">Ingresá al menos 3 caracteres para buscar artículos</span>
                    </div>
                </td>
            </tr>`;
        this.paginador.limpiar('');
    }

    cerrar(): void {
        if (!this.dialog) return;
        this.dialog.close();
    }

    async buscar(codigo: string = '', termino: string = '', pagina: number = 0): Promise<void> {
        if (!this.tbody) return;

        const listaPrecioId = this.options.getListaPrecioId();
        if (listaPrecioId == null) {
            this.tableWrap?.classList.add('is-empty');
            this.tbody.innerHTML = `<tr class="fila-vacia"><td colspan="4" class="modal-error-cell">Seleccioná una lista de precios para consultar artículos.</td></tr>`;
            this.paginador.limpiar('');
            return;
        }

        const codigoLimpio = codigo.trim();
        const terminoLimpio = termino.trim();
        if (codigoLimpio.length < 3 && terminoLimpio.length < 3) {
            this.mostrarMensajeMinimoCaracteres();
            return;
        }

        this.tableWrap?.classList.remove('is-empty');
        this.renderSkeletons(5);
        this.paginador.bloquear(true);

        try {
            const data = await this.options.articuloService.buscarArticulosPaginados(
                { listaPrecioId, codigo: codigoLimpio, termino: terminoLimpio },
                pagina,
                5
            );
            this.filtroCodigo = codigoLimpio;
            this.filtroTermino = terminoLimpio;

            this.renderizarFilas(data.contenido);

            if (data.totalElementos === 0) {
                this.paginador.limpiar('');
            } else {
                this.paginador.actualizar(data);
            }
        } catch (err) {
            const mensaje = err instanceof Error ? err.message : 'Error al obtener artículos del servidor.';
            this.tableWrap?.classList.add('is-empty');
            const tr = document.createElement('tr');
            tr.className = 'fila-vacia';
            const td = document.createElement('td');
            td.colSpan = 4;
            td.className = 'modal-error-cell';
            td.textContent = mensaje;
            tr.appendChild(td);
            this.tbody.replaceChildren(tr);
            this.paginador.limpiar('');
        } finally {
            this.tbody.classList.remove('is-loading');
            this.paginador.bloquear(false);
        }
    }

    private renderSkeletons(cantidad: number): void {
        if (!this.tbody) return;
        const fragment = document.createDocumentFragment();
        for (let i = 0; i < cantidad; i++) {
            const tr = document.createElement('tr');
            tr.className = 'fila-skeleton';
            const td = document.createElement('td');
            td.colSpan = 4;
            td.style.padding = '0';
            td.innerHTML = `
                <div class="skeleton-row" style="padding: 10px 14px; gap: 14px; align-items: center; height: 46px; box-sizing: border-box;">
                    <span class="skeleton" style="width: 85px; height: 13px; flex: none;"></span>
                    <span class="skeleton" style="height: 13px; flex: 1;"></span>
                    <span class="skeleton" style="width: 80px; height: 13px; flex: none;"></span>
                    <span class="skeleton" style="width: 86px; height: 26px; border-radius: 4px; flex: none;"></span>
                </div>`;
            tr.appendChild(td);
            fragment.appendChild(tr);
        }
        this.tbody.replaceChildren(fragment);
    }

    private renderizarFilas(articulos: ArticuloPrecioDTO[]): void {
        if (!this.tbody) return;
        this.tbody.replaceChildren();

        if (articulos.length === 0) {
            this.tableWrap?.classList.add('is-empty');
            this.tbody.innerHTML = `
                <tr class="fila-vacia">
                    <td colspan="4" class="modal-empty-cell">
                        <div class="modal-empty-content">
                            <span class="modal-empty-title">No se encontraron artículos</span>
                            <span class="modal-empty-subtitle">Revisá los filtros ingresados o probá con otra lista de precios.</span>
                        </div>
                    </td>
                </tr>`;
            return;
        }

        this.tableWrap?.classList.remove('is-empty');

        const fragment = document.createDocumentFragment();
        for (const articulo of articulos) {
            const tr = document.createElement('tr');
            tr.className = 'articulo-row';
            tr.tabIndex = 0;
            tr.setAttribute('role', 'button');
            tr.setAttribute('aria-label', `Seleccionar ${articulo.denominacion}`);

            const tdCodigo = document.createElement('td');
            tdCodigo.className = 'mono';
            tdCodigo.textContent = articulo.codigo || '—';

            const tdDetalle = document.createElement('td');
            const titulo = document.createElement('div');
            titulo.className = 'articulo-row-titulo';
            titulo.textContent = articulo.denominacion;
            const sub = document.createElement('div');
            sub.className = 'articulo-row-sub';
            sub.textContent = [articulo.marca, articulo.listaPrecioDenominacion].filter(Boolean).join(' · ') || '—';
            tdDetalle.append(titulo, sub);

            const tdPrecio = document.createElement('td');
            tdPrecio.className = 'num';
            tdPrecio.textContent = articulo.precioVenta != null ? formatearMoneda(articulo.precioVenta) : '—';

            const tdAccion = document.createElement('td');
            tdAccion.style.textAlign = 'right';

            const btnElegir = document.createElement('button');
            btnElegir.type = 'button';
            btnElegir.className = 'btn btn-secondary btn--sm btn-elegir-articulo';
            btnElegir.innerHTML = 'Seleccionar <svg class="icon" style="width:12px;height:12px;" aria-hidden="true"><use href="#i-next"/></svg>';
            btnElegir.tabIndex = -1;

            const seleccionar = () => {
                this.options.onSeleccionarArticulo(articulo);
                this.cerrar();
            };

            tr.addEventListener('click', () => seleccionar());
            tr.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    seleccionar();
                }
            });

            tdAccion.appendChild(btnElegir);
            tr.append(tdCodigo, tdDetalle, tdPrecio, tdAccion);
            fragment.appendChild(tr);
        }

        this.tbody.appendChild(fragment);
    }
}
