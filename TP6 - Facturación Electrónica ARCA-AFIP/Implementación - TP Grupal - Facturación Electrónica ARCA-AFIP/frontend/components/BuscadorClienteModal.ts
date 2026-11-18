import { ClienteDTO } from '../models/api.dto';
import { ClienteService } from '../services/ClienteService';
import { PaginadorControl } from './PaginadorControl';

export interface BuscadorClienteOptions {
    clienteService: ClienteService;
    onSeleccionarCliente: (cliente: ClienteDTO) => void;
}

/**
 * Componente modal para buscar y seleccionar clientes con paginación integrada y unificada.
 * Permite filtrar por CUIT/CUIL, por Razón Social/Denominación o por ambos valores a la vez.
 */
export class BuscadorClienteModal {
    private readonly dialog: HTMLDialogElement | null;
    private readonly btnCerrar: HTMLButtonElement | null;
    private readonly inputCuit: HTMLInputElement | null;
    private readonly inputRazonSocial: HTMLInputElement | null;
    private readonly btnBuscar: HTMLButtonElement | null;
    private readonly tbody: HTMLTableSectionElement | null;
    private readonly barraPaginacion: HTMLElement | null;
    private readonly tableWrap: HTMLElement | null;
    private readonly paginador: PaginadorControl;

    private paginaActual = 0;
    private filtroCuit = '';
    private filtroRazonSocial = '';

    constructor(private readonly options: BuscadorClienteOptions) {
        this.dialog = document.getElementById('modal-cliente-explorer') as HTMLDialogElement | null;
        this.btnCerrar = document.getElementById('btnCerrarModalCliente') as HTMLButtonElement | null;
        this.inputCuit = document.getElementById('inputBuscarClienteCuit') as HTMLInputElement | null;
        this.inputRazonSocial = document.getElementById('inputBuscarClienteRazonSocial') as HTMLInputElement | null;
        this.btnBuscar = document.getElementById('btnBuscarCliente') as HTMLButtonElement | null;
        this.tbody = document.getElementById('tbody-clientes-explorer') as HTMLTableSectionElement | null;
        this.barraPaginacion = this.dialog?.querySelector('.pagination-bar') as HTMLElement | null;
        this.tableWrap = this.dialog?.querySelector('.dialog-table') as HTMLElement | null;

        this.paginador = new PaginadorControl({
            infoEl: document.getElementById('infoPaginacionCliente'),
            prevBtn: document.getElementById('btnPaginacionAnterior') as HTMLButtonElement | null,
            nextBtn: document.getElementById('btnPaginacionSiguiente') as HTMLButtonElement | null,
            etiquetaItem: 'clientes',
            onCambiarPagina: (p) => {
                void this.buscar(this.filtroCuit, this.filtroRazonSocial, p);
            }
        });

        this.initEvents();
    }

    private leerFiltrosActuales(): { cuit: string; razonSocial: string } {
        return {
            cuit: this.inputCuit?.value.trim() ?? '',
            razonSocial: this.inputRazonSocial?.value.trim() ?? ''
        };
    }

    private initEvents(): void {
        this.btnCerrar?.addEventListener('click', () => this.cerrar());

        // Cerrar al hacer clic en el backdrop oscuro nativo de dialog
        this.dialog?.addEventListener('click', (e) => {
            if (e.target === this.dialog) {
                this.cerrar();
            }
        });

        // Buscar al hacer clic en el botón
        this.btnBuscar?.addEventListener('click', () => {
            const { cuit, razonSocial } = this.leerFiltrosActuales();
            void this.buscar(cuit, razonSocial, 0);
        });

        // Buscar en tiempo real o con Enter en cualquiera de las dos cajas de texto
        let debounceTimer: number | undefined;
        const inputs = [this.inputCuit, this.inputRazonSocial].filter(
            (el): el is HTMLInputElement => el !== null
        );

        for (const input of inputs) {
            input.addEventListener('input', () => {
                if (debounceTimer !== undefined) window.clearTimeout(debounceTimer);
                debounceTimer = window.setTimeout(() => {
                    const { cuit, razonSocial } = this.leerFiltrosActuales();
                    void this.buscar(cuit, razonSocial, 0);
                }, 250);
            });

            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    if (debounceTimer !== undefined) window.clearTimeout(debounceTimer);
                    const { cuit, razonSocial } = this.leerFiltrosActuales();
                    void this.buscar(cuit, razonSocial, 0);
                }
            });
        }
    }

    abrir(): void {
        if (!this.dialog) return;
        this.dialog.showModal();
        if (this.inputCuit) this.inputCuit.value = '';
        if (this.inputRazonSocial) this.inputRazonSocial.value = '';
        this.inputCuit?.focus();
        this.mostrarMensajeMinimoCaracteres();
    }

    private mostrarMensajeMinimoCaracteres(): void {
        if (!this.tbody) return;
        this.filtroCuit = '';
        this.filtroRazonSocial = '';
        this.tableWrap?.classList.add('is-empty');
        this.tbody.innerHTML = `
            <tr class="fila-vacia">
                <td colspan="3" class="modal-empty-cell">
                    <div class="modal-empty-content">
                        <span class="modal-empty-title">Ingresá al menos 3 caracteres en CUIT/CUIL o Razón social para buscar clientes</span>
                    </div>
                </td>
            </tr>`;
        this.paginador.limpiar('');
    }

    cerrar(): void {
        if (!this.dialog) return;
        this.dialog.close();
    }

    async buscar(cuit: string = '', razonSocial: string = '', pagina: number = 0): Promise<void> {
        if (!this.tbody) return;

        const cuitLimpio = cuit.trim();
        const razonSocialLimpia = razonSocial.trim();
        if (cuitLimpio.length < 3 && razonSocialLimpia.length < 3) {
            this.mostrarMensajeMinimoCaracteres();
            return;
        }

        // Quitamos la clase de vacío para que el thead y los skeletons se alineen prolijamente
        this.tableWrap?.classList.remove('is-empty');
        this.renderSkeletons(5);
        this.paginador.bloquear(true);

        try {
            const [data] = await Promise.all([
                this.options.clienteService.buscarClientes(
                    { cuit: cuitLimpio, razonSocial: razonSocialLimpia },
                    pagina,
                    5
                ),
                new Promise((resolve) => setTimeout(resolve, 180))
            ]);
            this.paginaActual = data.paginaActual;
            this.filtroCuit = cuitLimpio;
            this.filtroRazonSocial = razonSocialLimpia;

            this.renderizarFilas(data.contenido);

            if (data.totalElementos === 0) {
                this.paginador.limpiar('');
            } else {
                this.paginador.actualizar(data);
            }
        } catch (err) {
            const mensaje = err instanceof Error ? err.message : 'Error al obtener clientes del servidor.';
            this.tableWrap?.classList.add('is-empty');
            this.tbody.innerHTML = `<tr class="fila-vacia"><td colspan="3" class="modal-error-cell">${mensaje}</td></tr>`;
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
            td.colSpan = 3;
            td.style.padding = '0';
            td.innerHTML = `
                <div class="skeleton-row" style="padding: 10px 14px; gap: 14px; align-items: center; height: 46px; box-sizing: border-box;">
                    <span class="skeleton" style="width: 120px; height: 13px; flex: none;"></span>
                    <span class="skeleton" style="height: 13px; flex: 1;"></span>
                    <span class="skeleton" style="width: 86px; height: 26px; border-radius: 4px; flex: none;"></span>
                </div>`;
            tr.appendChild(td);
            fragment.appendChild(tr);
        }
        this.tbody.replaceChildren(fragment);
    }

    private renderizarFilas(clientes: ClienteDTO[]): void {
        if (!this.tbody) return;
        this.tbody.replaceChildren();

        if (clientes.length === 0) {
            this.tableWrap?.classList.add('is-empty');
            this.tbody.innerHTML = `
                <tr class="fila-vacia">
                    <td colspan="3" class="modal-empty-cell">
                        <div class="modal-empty-content">
                            <span class="modal-empty-title">No se encontraron clientes</span>
                            <span class="modal-empty-subtitle">Revisá el CUIT o razón social ingresada e intentá nuevamente.</span>
                        </div>
                    </td>
                </tr>`;
            return;
        }

        this.tableWrap?.classList.remove('is-empty');

        const fragment = document.createDocumentFragment();
        for (const cliente of clientes) {
            const tr = document.createElement('tr');
            tr.className = 'cliente-row';
            tr.tabIndex = 0;
            tr.setAttribute('role', 'button');
            tr.setAttribute('aria-label', `Seleccionar ${cliente.denominacion}`);

            const tdCuit = document.createElement('td');
            tdCuit.className = 'mono';
            tdCuit.textContent = cliente.cuitCuil || '—';

            const tdDenominacion = document.createElement('td');
            tdDenominacion.textContent = cliente.denominacion;

            const tdAccion = document.createElement('td');
            tdAccion.style.textAlign = 'right';

            // Botón interactivo de selección visible (con soporte hover integrado)
            const btnElegir = document.createElement('button');
            btnElegir.type = 'button';
            btnElegir.className = 'btn btn-secondary btn--sm btn-elegir-cliente';
            btnElegir.innerHTML = 'Seleccionar <svg class="icon" style="width:12px;height:12px;" aria-hidden="true"><use href="#i-next"/></svg>';
            btnElegir.tabIndex = -1;

            const seleccionar = () => {
                this.options.onSeleccionarCliente(cliente);
                this.cerrar();
            };

            // Selección intuitiva haciendo clic en cualquier parte de la fila
            tr.addEventListener('click', () => seleccionar());
            tr.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    seleccionar();
                }
            });

            tdAccion.appendChild(btnElegir);
            tr.append(tdCuit, tdDenominacion, tdAccion);
            fragment.appendChild(tr);
        }

        this.tbody.appendChild(fragment);
    }
}

