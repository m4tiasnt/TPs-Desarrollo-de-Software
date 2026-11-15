import { calcularSubtotalDetalle, calcularTotalesFactura, TotalesFactura } from '../utils/calculos-factura';
import { formatearMoneda } from '../utils/formatters';
import { setTexto } from '../utils/dom';

export interface FacturaDetalleUI {
    articuloId: number;
    listaPrecioArticuloId: number;
    descripcion?: string;
    cantidad: number;
    porcentajeBonificacion: number;
    precioUnitario: number;
    listaPrecioDenominacion: string;
    articuloDenominacion: string;
    articuloCodigo: string;
    articuloMarca?: string;
}

export type ModoFactura = 'nueva' | 'editar' | 'ver';

export interface ResultadoAgregarDetalle {
    /** true si el artículo ya estaba y se sumó a su renglón en vez de crear uno nuevo */
    fusionado: boolean;
    indice: number;
    /** cantidad final del renglón */
    cantidad: number;
}

export interface TablaDetallesOptions {
    tbody: HTMLTableSectionElement;
    wrapEl: HTMLElement;
    vacioEl: HTMLElement;
    errorEl: HTMLElement;
    tplEditable: HTMLTemplateElement;
    tplVer: HTMLTemplateElement;
    onCambioTotales: (totales: TotalesFactura, cantidad: number) => void;
    onAbrirMenuDetalle?: (idx: number, anchorBtn: HTMLButtonElement) => void;
    onMensajeFeedback?: (mensaje: string) => void;
}

/**
 * Componente que gestiona la tabla de detalles/artículos de la factura de venta.
 * Controla inputs de cantidad, bonificación, subtotales y avisos de estado vacío.
 */
export class TablaDetallesFactura {
    private detalles: FacturaDetalleUI[] = [];
    private modo: ModoFactura = 'nueva';

    constructor(private readonly options: TablaDetallesOptions) {}

    setModo(modo: ModoFactura): void {
        this.modo = modo;
    }

    mostrarEsqueletos(cantidad: number = 3): void {
        this.options.vacioEl.hidden = true;
        this.options.wrapEl.hidden = false;
        const fragment = document.createDocumentFragment();
        const esVer = this.modo === 'ver';
        for (let i = 0; i < cantidad; i++) {
            const tr = document.createElement('tr');
            tr.className = 'fila-skeleton';
            tr.innerHTML = `
                <td>
                    <span class="skeleton" style="width:65%;height:14px;margin-bottom:6px;display:block;"></span>
                    <span class="skeleton" style="width:35%;height:11px;display:block;"></span>
                </td>
                <td class="num"><span class="skeleton" style="width:30px;height:14px;margin-left:auto;display:block;"></span></td>
                <td class="num"><span class="skeleton" style="width:70px;height:14px;margin-left:auto;display:block;"></span></td>
                <td class="num"><span class="skeleton" style="width:30px;height:14px;margin-left:auto;display:block;"></span></td>
                <td class="num"><span class="skeleton" style="width:75px;height:14px;margin-left:auto;display:block;"></span></td>
                ${esVer ? '' : '<td><span class="skeleton" style="width:60px;height:24px;margin-left:auto;display:block;"></span></td>'}
            `;
            fragment.appendChild(tr);
        }
        this.options.tbody.replaceChildren(fragment);
    }

    setDetalles(detalles: FacturaDetalleUI[]): void {
        // Si la factura guardada trae el mismo artículo en varios renglones, se unifican.
        this.detalles = [];
        detalles.forEach((d) => this.fusionarOAgregar({ ...d }));
        this.render();
    }

    obtenerDetalles(): FacturaDetalleUI[] {
        return [...this.detalles];
    }

    /**
     * Agrega un artículo a la factura. Si el artículo ya está cargado, no crea un
     * renglón nuevo: le suma la cantidad al existente.
     */
    agregarDetalle(detalle: FacturaDetalleUI): ResultadoAgregarDetalle {
        const resultado = this.fusionarOAgregar(detalle);
        this.render();
        if (resultado.fusionado) this.resaltarFila(resultado.indice);
        return resultado;
    }

    private fusionarOAgregar(detalle: FacturaDetalleUI): ResultadoAgregarDetalle {
        const indice = (detalle.articuloId && detalle.articuloId > 0)
            ? this.detalles.findIndex((d) => d.articuloId === detalle.articuloId)
            : -1;
        if (indice === -1) {
            this.detalles.push(detalle);
            return { fusionado: false, indice: this.detalles.length - 1, cantidad: detalle.cantidad };
        }
        const existente = this.detalles[indice];
        existente.cantidad += detalle.cantidad > 0 ? detalle.cantidad : 1;
        return { fusionado: true, indice, cantidad: existente.cantidad };
    }

    private resaltarFila(idx: number): void {
        const fila = this.options.tbody.querySelectorAll('tr')[idx];
        if (!fila) return;
        fila.classList.add('fila-actualizada');
        fila.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        window.setTimeout(() => fila.classList.remove('fila-actualizada'), 1200);
    }

    quitarDetalle(indice: number): void {
        if (indice >= 0 && indice < this.detalles.length) {
            this.detalles.splice(indice, 1);
            this.render();
        }
    }

    actualizarDetalle(indice: number, cambios: Partial<FacturaDetalleUI>): void {
        if (indice >= 0 && indice < this.detalles.length) {
            this.detalles[indice] = { ...this.detalles[indice], ...cambios };
            this.render();
        }
    }

    render(): void {
        if (this.detalles.length === 0) {
            this.options.vacioEl.hidden = false;
            this.options.wrapEl.hidden = true;
            this.options.errorEl.hidden = true;
            this.options.tbody.replaceChildren();
            this.notificarTotales();
            return;
        }

        this.options.vacioEl.hidden = true;
        this.options.wrapEl.hidden = false;
        this.options.errorEl.hidden = true;

        const tpl = this.modo === 'ver' ? this.options.tplVer : this.options.tplEditable;
        this.options.tbody.replaceChildren();

        this.detalles.forEach((d, idx) => {
            const clone = tpl.content.cloneNode(true) as DocumentFragment;
            const tr = clone.querySelector('tr') as HTMLTableRowElement;

            setTexto(tr, '[data-campo="articuloDenominacion"]', d.articuloDenominacion);
            const meta = [d.articuloCodigo, d.listaPrecioDenominacion].filter(Boolean).join(' · ');
            setTexto(tr, '[data-campo="articuloMeta"]', meta || '—');

            const descEl = tr.querySelector('[data-campo="articuloDescripcion"]') as HTMLElement | null;
            if (descEl) {
                const desc = (d.descripcion || '').trim();
                const denom = (d.articuloDenominacion || '').trim();
                if (desc && desc.toLowerCase() !== denom.toLowerCase()) {
                    descEl.textContent = desc;
                    descEl.hidden = false;
                } else {
                    descEl.textContent = '';
                    descEl.hidden = true;
                }
            }

            setTexto(tr, '[data-campo="importeSubtotal"]', formatearMoneda(calcularSubtotalDetalle(d)));

            if (this.modo === 'ver') {
                setTexto(tr, '[data-campo="cantidad"]', String(d.cantidad));
                setTexto(tr, '[data-campo="precioUnitario"]', formatearMoneda(d.precioUnitario));
                setTexto(tr, '[data-campo="porcentajeBonificacion"]', String(d.porcentajeBonificacion ?? 0));
                this.options.tbody.appendChild(tr);
                return;
            }

            // Modo edición
            const inpArtId = tr.querySelector('[data-campo="articuloId"]') as HTMLInputElement | null;
            if (inpArtId) inpArtId.value = String(d.articuloId);

            const inpListaId = tr.querySelector('[data-campo="listaPrecioArticuloId"]') as HTMLInputElement | null;
            if (inpListaId) inpListaId.value = String(d.listaPrecioArticuloId);

            const inpPrecio = tr.querySelector('input[name="precioUnitario"]') as HTMLInputElement | null;
            if (inpPrecio) inpPrecio.value = formatearMoneda(d.precioUnitario);

            const inpCant = tr.querySelector('input[name="cantidad"]') as HTMLInputElement;
            inpCant.value = String(d.cantidad);

            const btnDecrementar = tr.querySelector('[data-accion="decrementar"]') as HTMLButtonElement | null;
            const btnIncrementar = tr.querySelector('[data-accion="incrementar"]') as HTMLButtonElement | null;

            const actualizarEstadoDecrementar = () => {
                if (btnDecrementar) {
                    btnDecrementar.disabled = this.detalles[idx].cantidad <= 1;
                }
            };

            const modificarCantidad = (delta: number) => {
                const actual = this.detalles[idx].cantidad || 1;
                const nueva = Math.max(1, actual + delta);
                this.detalles[idx].cantidad = nueva;
                inpCant.value = String(nueva);
                actualizarEstadoDecrementar();
                this.actualizarFila(idx);
                this.notificarTotales();
                if (delta > 0) {
                    this.options.onMensajeFeedback?.(
                        `Se ha agregado una unidad del artículo seleccionado: "${this.detalles[idx].articuloDenominacion}".`
                    );
                }
            };

            btnDecrementar?.addEventListener('click', (e) => {
                e.preventDefault();
                modificarCantidad(-1);
            });

            btnIncrementar?.addEventListener('click', (e) => {
                e.preventDefault();
                modificarCantidad(1);
            });

            inpCant.addEventListener('input', () => {
                const val = parseFloat(inpCant.value);
                this.detalles[idx].cantidad = Math.max(1, val || 1);
                actualizarEstadoDecrementar();
                this.actualizarFila(idx);
                this.notificarTotales();
            });

            inpCant.addEventListener('blur', () => {
                inpCant.value = String(this.detalles[idx].cantidad);
            });

            actualizarEstadoDecrementar();

            const inpBonif = tr.querySelector('input[name="porcentajeBonificacion"]') as HTMLInputElement;
            inpBonif.value = String(d.porcentajeBonificacion ?? 0);
            inpBonif.addEventListener('input', () => {
                const val = parseFloat(inpBonif.value) || 0;
                this.detalles[idx].porcentajeBonificacion = Math.min(100, Math.max(0, val));
                this.actualizarFila(idx);
                this.notificarTotales();
            });

            const btnMenu = tr.querySelector('[data-accion="menu"]') as HTMLButtonElement | null;
            btnMenu?.addEventListener('click', (e) => {
                e.stopPropagation();
                this.options.onAbrirMenuDetalle?.(idx, btnMenu);
            });

            const btnQuitar = tr.querySelector('[data-accion="quitar"]') as HTMLButtonElement | null;
            btnQuitar?.addEventListener('click', () => {
                this.quitarDetalle(idx);
            });

            this.options.tbody.appendChild(tr);
        });

        this.notificarTotales();
    }

    private actualizarFila(idx: number): void {
        const fila = this.options.tbody.querySelectorAll('tr')[idx];
        if (!fila) return;
        const celdaSubtotal = fila.querySelector('[data-campo="importeSubtotal"]');
        if (celdaSubtotal) {
            celdaSubtotal.textContent = formatearMoneda(calcularSubtotalDetalle(this.detalles[idx]));
        }
    }

    private notificarTotales(): void {
        const totales = calcularTotalesFactura(this.detalles);
        this.options.onCambioTotales(totales, this.detalles.length);
    }
}
