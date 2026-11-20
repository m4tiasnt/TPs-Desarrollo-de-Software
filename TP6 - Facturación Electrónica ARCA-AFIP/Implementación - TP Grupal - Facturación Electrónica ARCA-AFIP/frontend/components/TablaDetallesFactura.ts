import { TotalesFactura } from '../utils/calculos-factura';
import { formatearMoneda } from '../utils/formatters';
import { setTexto } from '../utils/dom';
import { Factura, ResultadoAgregarDetalle } from '../models/Factura';
import { FacturaDetalle, FacturaDetalleProps } from '../models/FacturaDetalle';

export type { ResultadoAgregarDetalle, FacturaDetalleProps };

export type ModoFactura = 'nueva' | 'editar' | 'ver';

export interface TablaDetallesOptions {
    factura?: Factura;
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
 * Componente visual que gestiona la tabla de detalles/artículos de un objeto Factura.
 * Delega el manejo de los objetos FacturaDetalle al modelo Factura y actualiza la grilla del DOM.
 */
export class TablaDetallesFactura {
    private factura: Factura;
    private modo: ModoFactura = 'nueva';

    constructor(private readonly options: TablaDetallesOptions) {
        this.factura = options.factura ?? new Factura();
    }

    setModo(modo: ModoFactura): void {
        if (this.modo === modo) return;
        this.modo = modo;
        this.render();
    }

    setFactura(factura: Factura): void {
        this.factura = factura;
        this.render();
    }

    getFactura(): Factura {
        return this.factura;
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
                <td class="num"><span class="skeleton" style="width:65px;height:14px;margin-left:auto;display:block;"></span></td>
                <td class="num"><span class="skeleton" style="width:75px;height:14px;margin-left:auto;display:block;"></span></td>
                ${esVer ? '' : '<td><span class="skeleton" style="width:60px;height:24px;margin-left:auto;display:block;"></span></td>'}
            `;
            fragment.appendChild(tr);
        }
        this.options.tbody.replaceChildren(fragment);
    }

    setDetalles(detalles: (FacturaDetalle | FacturaDetalleProps)[]): void {
        const instancias = detalles.map((d) =>
            d instanceof FacturaDetalle ? d : new FacturaDetalle(d)
        );
        this.factura.setDetalles(instancias);
        this.render();
    }

    obtenerDetalles(): FacturaDetalle[] {
        return this.factura.obtenerDetalles();
    }

    /**
     * Agrega un objeto FacturaDetalle al objeto Factura.
     * Si el artículo ya está cargado, no crea un renglón nuevo: incrementa su cantidad.
     */
    agregarDetalle(detalle: FacturaDetalle | FacturaDetalleProps): ResultadoAgregarDetalle {
        const instancia = detalle instanceof FacturaDetalle ? detalle : new FacturaDetalle(detalle);
        const resultado = this.factura.agregarDetalle(instancia);
        this.render();
        if (resultado.fusionado) this.resaltarFila(resultado.indice);
        return resultado;
    }

    private resaltarFila(idx: number): void {
        const fila = this.options.tbody.querySelectorAll('tr')[idx];
        if (!fila) return;
        fila.classList.add('fila-actualizada');
        fila.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        window.setTimeout(() => fila.classList.remove('fila-actualizada'), 1200);
    }

    quitarDetalle(indice: number): void {
        if (indice >= 0 && indice < this.factura.detalles.length) {
            this.factura.quitarDetalle(indice);
            this.render();
        }
    }

    actualizarDetalle(indice: number, cambios: Partial<FacturaDetalleProps>): ResultadoAgregarDetalle | null {
        if (indice >= 0 && indice < this.factura.detalles.length) {
            const resultado = this.factura.actualizarDetalle(indice, cambios);
            this.render();
            if (resultado?.fusionado) this.resaltarFila(resultado.indice);
            return resultado;
        }
        return null;
    }

    render(): void {
        const detalles = this.factura.detalles;
        if (detalles.length === 0) {
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

        detalles.forEach((d, idx) => {
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

            setTexto(tr, '[data-campo="importeIva"]', formatearMoneda(d.calcularIva()));
            setTexto(tr, '[data-campo="importeSubtotal"]', formatearMoneda(d.calcularSubtotal()));

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
                const actual = this.factura.detalles[idx];
                if (btnDecrementar && actual) {
                    btnDecrementar.disabled = actual.cantidad <= 1;
                }
            };

            const modificarCantidad = (delta: number) => {
                const actual = this.factura.detalles[idx];
                if (!actual) return;
                const nueva = Math.max(1, (actual.cantidad || 1) + delta);
                actual.setCantidad(nueva);
                inpCant.value = String(actual.cantidad);
                actualizarEstadoDecrementar();
                this.actualizarFila(idx);
                this.notificarTotales();
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
                const actual = this.factura.detalles[idx];
                if (!actual) return;
                const val = parseFloat(inpCant.value);
                actual.setCantidad(Math.max(1, val || 1));
                actualizarEstadoDecrementar();
                this.actualizarFila(idx);
                this.notificarTotales();
            });

            inpCant.addEventListener('blur', () => {
                const actual = this.factura.detalles[idx];
                if (actual) inpCant.value = String(actual.cantidad);
            });

            actualizarEstadoDecrementar();

            const inpBonif = tr.querySelector('input[name="porcentajeBonificacion"]') as HTMLInputElement;
            inpBonif.value = String(d.porcentajeBonificacion ?? 0);
            inpBonif.addEventListener('input', () => {
                const actual = this.factura.detalles[idx];
                if (!actual) return;
                const val = parseFloat(inpBonif.value) || 0;
                actual.setPorcentajeBonificacion(val);
                this.actualizarFila(idx);
                this.notificarTotales();
            });

            inpBonif.addEventListener('blur', () => {
                const actual = this.factura.detalles[idx];
                if (actual) inpBonif.value = String(actual.porcentajeBonificacion);
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
        const detalle = this.factura.detalles[idx];
        if (!fila || !detalle) return;
        const celdaIva = fila.querySelector('[data-campo="importeIva"]');
        if (celdaIva) {
            celdaIva.textContent = formatearMoneda(detalle.calcularIva());
        }
        const celdaSubtotal = fila.querySelector('[data-campo="importeSubtotal"]');
        if (celdaSubtotal) {
            celdaSubtotal.textContent = formatearMoneda(detalle.calcularSubtotal());
        }
    }

    private notificarTotales(): void {
        const totales = this.factura.calcularTotales();
        this.options.onCambioTotales(totales, this.factura.detalles.length);
    }
}
