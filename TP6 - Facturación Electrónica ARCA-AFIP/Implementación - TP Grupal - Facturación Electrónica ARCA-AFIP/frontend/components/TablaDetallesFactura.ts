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

export interface TablaDetallesOptions {
    tbody: HTMLTableSectionElement;
    wrapEl: HTMLElement;
    vacioEl: HTMLElement;
    errorEl: HTMLElement;
    tplEditable: HTMLTemplateElement;
    tplVer: HTMLTemplateElement;
    onCambioTotales: (totales: TotalesFactura, cantidad: number) => void;
    onAbrirMenuDetalle?: (idx: number, anchorBtn: HTMLButtonElement) => void;
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
        this.detalles = [...detalles];
        this.render();
    }

    obtenerDetalles(): FacturaDetalleUI[] {
        return [...this.detalles];
    }

    agregarDetalle(detalle: FacturaDetalleUI): void {
        this.detalles.push(detalle);
        this.render();
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
            inpCant.addEventListener('input', () => {
                this.detalles[idx].cantidad = Math.max(1, parseFloat(inpCant.value) || 1);
                this.actualizarFila(idx);
                this.notificarTotales();
            });

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
