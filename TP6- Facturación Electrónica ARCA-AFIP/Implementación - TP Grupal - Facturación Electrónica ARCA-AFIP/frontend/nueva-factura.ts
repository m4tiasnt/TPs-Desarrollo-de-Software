import {
    ClienteDTO,
    EstadoFactura,
    FacturaVentaDTO,
    FacturaVentaRequestDTO,
    InformarArcaResultadoDTO
} from './models/api.dto';
import { ApiError, FacturaService } from './services/FacturaService';
import {
    aplicarBadgeEstado,
    formatearFecha,
    formatearMoneda,
    obtenerIdentificadorFactura
} from './utils/formatters';
import { TotalesFactura } from './utils/calculos-factura';
import { setTexto } from './utils/dom';
import { BuscadorClienteModal } from './components/BuscadorClienteModal';
import { BuscadorArticuloCombobox } from './components/BuscadorArticuloCombobox';
import { ModoFactura, TablaDetallesFactura, FacturaDetalleUI } from './components/TablaDetallesFactura';

// ─── Servicios y Estado ─────────────────────────────────────────────────────
const facturaService = new FacturaService('http://localhost:8080/api/facturas');

let modoActual: ModoFactura = 'nueva';
let facturaId: number | null = null;
let facturaActual: FacturaVentaDTO | null = null;
let listaBaseId: number | null = null;
let snapshotInicial = '';
let detalleEditando: number | null = null;

// ─── Referencias DOM ────────────────────────────────────────────────────────
const formFactura = document.getElementById('form-factura') as HTMLFormElement;
const alertaError = document.getElementById('alerta-error') as HTMLDivElement;
const alertaErrorTexto = document.getElementById('alerta-error-texto') as HTMLSpanElement;
const btnCerrarAlerta = document.getElementById('btn-cerrar-alerta') as HTMLButtonElement;
const linkVolver = document.getElementById('link-volver') as HTMLAnchorElement;
const btnCancelar = document.getElementById('btn-cancelar') as HTMLButtonElement;
const btnGuardar = document.getElementById('btn-guardar') as HTMLButtonElement;
const btnInformar = document.getElementById('btn-informar') as HTMLButtonElement;
const btnEditar = document.getElementById('btn-editar') as HTMLAnchorElement;
const btnMasAcciones = document.getElementById('btn-mas-acciones') as HTMLButtonElement;
const menuFactura = document.getElementById('menu-factura') as HTMLUListElement;
const tituloPagina = document.getElementById('titulo-pagina') as HTMLHeadingElement;
const badgeEstado = document.getElementById('badge-estado') as HTMLElement;
const subtituloPagina = document.getElementById('subtitulo-pagina') as HTMLParagraphElement;

const selectCondicionIva = document.getElementById('condicionIvaId') as HTMLSelectElement;
const inputClienteId = document.getElementById('clienteId') as HTMLInputElement;
const clienteDisplay = document.getElementById('clienteDisplay') as HTMLInputElement;
const btnAbrirModalCliente = document.getElementById('btnAbrirModalCliente') as HTMLButtonElement;
const btnLimpiarCliente = document.getElementById('btn-limpiar-cliente') as HTMLButtonElement;
const clienteOpcional = document.getElementById('cliente-opcional') as HTMLSpanElement;
const hintCliente = document.getElementById('hint-cliente') as HTMLParagraphElement;
const errorCliente = document.getElementById('error-clienteId') as HTMLParagraphElement;
const selectPuntoVenta = document.getElementById('puntoVentaId') as HTMLSelectElement;
const selectTipoMoneda = document.getElementById('tipoMonedaId') as HTMLSelectElement;
const selectListaBase = document.getElementById('listaPrecioId') as HTMLSelectElement;
const textareaObservaciones = document.getElementById('observaciones') as HTMLTextAreaElement;

const buscadorArticulo = document.getElementById('buscador-articulo') as HTMLInputElement;
const listaArticulos = document.getElementById('lista-articulos') as HTMLUListElement;
const tplOpcionArticulo = document.getElementById('tpl-opcion-articulo') as HTMLTemplateElement;

const detallesTbody = document.getElementById('detalles-tbody') as HTMLTableSectionElement;
const detallesWrap = document.getElementById('detalles-wrap') as HTMLElement;
const detallesVacio = document.getElementById('detalles-vacio') as HTMLElement;
const errorDetalles = document.getElementById('error-detalles') as HTMLElement;
const tplDetalle = document.getElementById('tpl-detalle') as HTMLTemplateElement;
const tplDetalleVer = document.getElementById('tpl-detalle-ver') as HTMLTemplateElement;
const menuDetalle = document.getElementById('menu-detalle') as HTMLUListElement;

const resumenCantidad = document.getElementById('resumen-cantidad') as HTMLElement;
const resumenNeto = document.getElementById('resumen-neto') as HTMLElement;
const resumenIva = document.getElementById('resumen-iva') as HTMLElement;
const resumenTotal = document.getElementById('resumen-total') as HTMLElement;

// Diálogos modales
const dlgEditarLista = document.getElementById('dlg-editar-lista') as HTMLDialogElement;
const editListaPrecioId = document.getElementById('edit-listaPrecioId') as HTMLSelectElement;
const dlgEditarDescripcion = document.getElementById('dlg-editar-descripcion') as HTMLDialogElement;
const editDescripcion = document.getElementById('edit-descripcion') as HTMLInputElement;
const dlgDescartar = document.getElementById('dlg-descartar') as HTMLDialogElement;
const dlgCambiarPuntoVenta = document.getElementById('dlg-cambiar-punto-venta') as HTMLDialogElement;
const dlgConflicto = document.getElementById('dlg-conflicto') as HTMLDialogElement;
const dlgInformar = document.getElementById('dlg-informar-arca') as HTMLDialogElement;

// ─── Componentes ────────────────────────────────────────────────────────────
const tablaDetalles = new TablaDetallesFactura({
    tbody: detallesTbody,
    wrapEl: detallesWrap,
    vacioEl: detallesVacio,
    errorEl: errorDetalles,
    tplEditable: tplDetalle,
    tplVer: tplDetalleVer,
    onCambioTotales: (totales: TotalesFactura, cantidad: number) => {
        resumenCantidad.textContent = `${cantidad} ${cantidad === 1 ? 'artículo' : 'artículos'}`;
        resumenNeto.textContent = formatearMoneda(totales.neto);
        resumenIva.textContent = formatearMoneda(totales.iva);
        resumenTotal.textContent = formatearMoneda(totales.total);
    },
    onAbrirMenuDetalle: (idx, anchor) => {
        abrirMenuDetalle(idx, anchor);
    }
});

const comboboxArticulos = new BuscadorArticuloCombobox({
    inputEl: buscadorArticulo,
    listaEl: listaArticulos,
    tplOpcion: tplOpcionArticulo,
    facturaService,
    getListaPrecioId: () => listaBaseId,
    onSeleccionarArticulo: (art) => {
        tablaDetalles.agregarDetalle({
            articuloId: art.id,
            listaPrecioArticuloId: art.listaPrecioArticuloId,
            descripcion: undefined,
            cantidad: 1,
            porcentajeBonificacion: 0,
            precioUnitario: art.precioVenta ?? 0,
            listaPrecioDenominacion: art.listaPrecioDenominacion,
            articuloDenominacion: art.denominacion,
            articuloCodigo: art.codigo,
            articuloMarca: art.marca
        });
    },
    onError: (msj) => mostrarAlertaError(msj)
});

const modalCliente = new BuscadorClienteModal({
    facturaService,
    onSeleccionarCliente: (cliente: ClienteDTO) => {
        inputClienteId.value = String(cliente.id);
        clienteDisplay.value = `${cliente.denominacion} (${cliente.cuitCuil})`;
        errorCliente.hidden = true;
        clienteDisplay.classList.remove('is-invalid');
        btnLimpiarCliente.hidden = false;
    }
});

// ─── Helpers de Alertas ─────────────────────────────────────────────────────
function mostrarAlertaError(mensaje: string): void {
    if (!alertaError) return;
    alertaErrorTexto.textContent = mensaje;
    alertaError.hidden = false;
    alertaError.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function ocultarAlertaError(): void {
    if (alertaError) alertaError.hidden = true;
}

// ─── Menú Contextual de Detalle ─────────────────────────────────────────────
let menuDetalleIdx: number | null = null;
let menuDetalleToggle: HTMLButtonElement | null = null;

function abrirMenuDetalle(idx: number, toggle: HTMLButtonElement): void {
    if (menuDetalleIdx === idx && !menuDetalle.hidden) {
        cerrarMenuDetalle();
        return;
    }
    menuDetalleIdx = idx;
    menuDetalleToggle = toggle;
    menuDetalle.hidden = false;

    const btn = toggle.getBoundingClientRect();
    const izquierda = Math.max(8, Math.min(btn.right - 190, window.innerWidth - 198));
    let arriba = btn.bottom + 4;
    if (arriba + 90 > window.innerHeight - 8) arriba = Math.max(8, btn.top - 94);
    menuDetalle.style.left = `${izquierda}px`;
    menuDetalle.style.top = `${arriba}px`;
    toggle.setAttribute('aria-expanded', 'true');
}

function cerrarMenuDetalle(): void {
    if (menuDetalle.hidden) return;
    menuDetalle.hidden = true;
    menuDetalleIdx = null;
    if (menuDetalleToggle) {
        menuDetalleToggle.setAttribute('aria-expanded', 'false');
        menuDetalleToggle = null;
    }
}

// ─── Carga de Catálogos y Factura ───────────────────────────────────────────
async function cargarCatalogos(): Promise<void> {
    try {
        const [listas, puntosVenta, condiciones, monedas] = await Promise.all([
            facturaService.obtenerListasPrecio(),
            facturaService.obtenerPuntosVenta(),
            facturaService.obtenerCondicionesIva(),
            facturaService.obtenerTiposMoneda()
        ]);

        selectListaBase.replaceChildren(new Option('Seleccionar…', ''));
        listas.forEach((l) => selectListaBase.appendChild(new Option(l.denominacion, String(l.id))));
        if (listas.length > 0) {
            selectListaBase.value = String(listas[0].id);
            listaBaseId = listas[0].id;
        }

        selectPuntoVenta.replaceChildren(new Option('Seleccionar…', ''));
        puntosVenta.forEach((pv) => {
            const num = String(pv.numero).padStart(4, '0');
            selectPuntoVenta.appendChild(new Option(`${num} · ${pv.descripcion}`, String(pv.id)));
        });

        selectCondicionIva.replaceChildren(new Option('Seleccionar…', ''));
        condiciones.forEach((c) => selectCondicionIva.appendChild(new Option(c.denominacion, String(c.id))));

        selectTipoMoneda.replaceChildren(new Option('Seleccionar…', ''));
        monedas.forEach((m) => selectTipoMoneda.appendChild(new Option(m.denominacion, String(m.id))));
        if (monedas.length > 0) selectTipoMoneda.value = String(monedas[0].id);
    } catch (e) {
        mostrarAlertaError(e instanceof Error ? e.message : 'Error al cargar catálogos del servidor');
    }
}

async function cargarFacturaExistente(id: number): Promise<void> {
    tablaDetalles.mostrarEsqueletos(3);
    try {
        const [f] = await Promise.all([
            facturaService.obtenerDetalle(id),
            new Promise((resolve) => setTimeout(resolve, 180))
        ]);
        facturaActual = f;
        document.body.dataset.estado = f.estado;
        aplicarBadgeEstado(badgeEstado, f.estado);

        // Título y subtítulo significativos
        const nroIdentificador = f.numeroComprobante || (f.id ? `#${f.id}` : '');
        if (modoActual === 'ver') {
            tituloPagina.textContent = f.numeroComprobante ? `Factura ${f.numeroComprobante}` : `Factura #${f.id}`;
        } else if (modoActual === 'editar') {
            tituloPagina.textContent = `Editar factura ${nroIdentificador}`;
        }
        const denomLimpia = f.clienteDenominacion?.trim();
        const tieneClienteRegistrado = Boolean(f.clienteId && denomLimpia && denomLimpia !== 'Consumidor Final');

        subtituloPagina.textContent = tieneClienteRegistrado ? `Cliente: ${denomLimpia}` : 'Cliente no registrado';

        // Cargar campos de lectura / edición
        const clienteEl = document.querySelector('[data-campo="clienteDenominacion"]');
        if (clienteEl) {
            if (tieneClienteRegistrado) {
                clienteEl.textContent = denomLimpia;
            } else {
                clienteEl.innerHTML = '<span class="muted">Cliente no registrado</span>';
            }
        }
        setTexto(document.body, '[data-campo="clienteCuit"]', tieneClienteRegistrado && f.clienteCuitCuil ? f.clienteCuitCuil : '—');
        setTexto(document.body, '[data-campo="condicionIva"]', f.condicionIva || '—');
        setTexto(document.body, '[data-campo="numeroFactura"]', f.numeroComprobante || (f.id ? `Borrador #${f.id}` : '—'));
        setTexto(document.body, '[data-campo="fechaEmision"]', formatearFecha(f.fechaEmision));
        const caeTexto = f.cae ? `${f.cae} · Vto: ${formatearFecha(f.caeFechaVencimiento)}` : '—';
        setTexto(document.body, '[data-campo="caeInfo"]', caeTexto);
        setTexto(document.body, '[data-campo="moneda"]', f.tipoMoneda ? `${f.tipoMoneda} (${f.tipoMonedaSimbolo || '$'})` : 'Pesos Argentinos ($)');
        setTexto(document.body, '[data-campo="puntoVenta"]', `${String(f.puntoVentaNumero || 1).padStart(4, '0')} · ${f.puntoVentaDescripcion || 'Casa Central'}`);
        setTexto(document.body, '[data-campo="fechaAlta"]', formatearFecha(f.fechaAlta));
        setTexto(document.body, '[data-campo="fechaModificacion"]', formatearFecha(f.fechaModificacion));
        setTexto(document.body, '[data-campo="observaciones"]', f.observaciones || '—');

        // Banners informativos según estado (rechazo ARCA / en proceso)
        setTexto(document.body, '[data-campo="motivoRechazo"]', f.motivoRechazo || f.resultadoAfip || 'Factura rechazada por ARCA. Revisá los datos del cliente y los detalles antes de volver a informar.');
        setTexto(document.body, '[data-campo="fechaIntento"]', formatearFecha(f.fechaModificacion || f.fechaAlta));

        if (inputClienteId) inputClienteId.value = tieneClienteRegistrado && f.clienteId ? String(f.clienteId) : '';
        if (clienteDisplay) {
            if (tieneClienteRegistrado) {
                clienteDisplay.value = f.clienteCuitCuil ? `${denomLimpia} (${f.clienteCuitCuil})` : denomLimpia;
            } else {
                clienteDisplay.value = '';
            }
        }
        if (selectCondicionIva) selectCondicionIva.value = String(f.condicionIvaId);
        if (selectPuntoVenta) selectPuntoVenta.value = String(f.puntoVentaId);
        if (selectTipoMoneda) selectTipoMoneda.value = String(f.tipoMonedaId);
        if (textareaObservaciones) textareaObservaciones.value = f.observaciones || '';

        const detalles: FacturaDetalleUI[] = (f.detalles || []).map((d) => ({
            articuloId: d.articuloId || 0,
            listaPrecioArticuloId: d.listaPrecioArticuloId || 0,
            descripcion: d.descripcion || undefined,
            cantidad: d.cantidad,
            porcentajeBonificacion: d.porcentajeBonificacion || 0,
            precioUnitario: d.precioUnitario,
            listaPrecioDenominacion: d.listaPrecio || '',
            articuloDenominacion: d.denominacionArticulo || '',
            articuloCodigo: d.codigoArticulo || '',
            articuloMarca: undefined
        }));

        tablaDetalles.setDetalles(detalles);
        snapshotInicial = serializarEstado();
    } catch (e) {
        mostrarAlertaError(e instanceof Error ? e.message : 'Error al cargar la factura');
    }
}

// ─── Validación y Serialización ─────────────────────────────────────────────
function esConsumidorFinal(): boolean {
    const opt = selectCondicionIva.selectedOptions[0];
    return opt ? opt.textContent.toLowerCase().includes('consumidor final') : false;
}

function validar(): boolean {
    let ok = true;
    const detalles = tablaDetalles.obtenerDetalles();
    if (detalles.length === 0) {
        errorDetalles.hidden = false;
        ok = false;
    } else {
        errorDetalles.hidden = true;
    }

    if (!selectCondicionIva.value || !selectPuntoVenta.value || !selectTipoMoneda.value) {
        ok = false;
    }

    const tieneCliente = Boolean(inputClienteId.value || clienteDisplay.value);
    if (!tieneCliente && !esConsumidorFinal()) {
        errorCliente.hidden = false;
        clienteDisplay.classList.add('is-invalid');
        ok = false;
    } else {
        errorCliente.hidden = true;
        clienteDisplay.classList.remove('is-invalid');
    }

    if (!ok) mostrarAlertaError('Revisá los datos marcados antes de guardar.');
    return ok;
}

function construirDTO(): FacturaVentaRequestDTO {
    const cid = inputClienteId.value ? parseInt(inputClienteId.value, 10) : undefined;
    return {
        clienteId: Number.isFinite(cid as number) ? cid : undefined,
        condicionIvaId: parseInt(selectCondicionIva.value, 10),
        puntoVentaId: parseInt(selectPuntoVenta.value, 10),
        tipoMonedaId: parseInt(selectTipoMoneda.value, 10),
        observaciones: textareaObservaciones.value.trim() || undefined,
        detalles: tablaDetalles.obtenerDetalles().map((r) => ({
            articuloId: r.articuloId,
            listaPrecioArticuloId: r.listaPrecioArticuloId,
            descripcion: r.descripcion,
            cantidad: r.cantidad,
            porcentajeBonificacion: r.porcentajeBonificacion
        }))
    };
}

function serializarEstado(): string {
    return JSON.stringify(construirDTO());
}

function hayCambios(): boolean {
    return serializarEstado() !== snapshotInicial;
}

// ─── Guardar ────────────────────────────────────────────────────────────────
async function guardar(): Promise<void> {
    if (!validar()) return;
    ocultarAlertaError();

    btnGuardar.disabled = true;
    const span = btnGuardar.querySelector('span');
    const textoOriginal = span?.textContent ?? 'Guardar factura';
    if (span) span.textContent = 'Guardando…';

    try {
        const dto = construirDTO();
        if (modoActual === 'editar' && facturaId != null) {
            await facturaService.actualizarFactura(facturaId, dto);
        } else {
            await facturaService.crearFactura(dto);
        }
        snapshotInicial = serializarEstado();
        window.location.href = 'index.html';
    } catch (e) {
        if (e instanceof ApiError && e.status === 409) {
            dlgConflicto.showModal();
        } else {
            mostrarAlertaError(e instanceof Error ? e.message : 'No se pudo guardar la factura.');
        }
    } finally {
        btnGuardar.disabled = false;
        if (span) span.textContent = textoOriginal;
    }
}

// ─── Diálogos de Acciones (Anular / Eliminar / ARCA) ─────────────────────────
function abrirConfirmacion(dlg: HTMLDialogElement, textoIdentificador: string): Promise<boolean> {
    dlg.querySelectorAll('[data-campo="identificador"]').forEach((el) => {
        el.textContent = textoIdentificador;
    });
    return new Promise((resolve) => {
        const alCerrar = () => {
            dlg.removeEventListener('close', alCerrar);
            resolve(dlg.returnValue === 'confirmar');
        };
        dlg.addEventListener('close', alCerrar);
        dlg.returnValue = 'cancelar';
        dlg.showModal();
    });
}

async function anularDesdeDetalle(): Promise<void> {
    if (facturaId == null || !facturaActual) return;
    if (!(await abrirConfirmacion(document.getElementById('dlg-anular') as HTMLDialogElement, obtenerIdentificadorFactura(facturaActual)))) return;
    try {
        await facturaService.anularFactura(facturaId);
        window.location.href = 'index.html';
    } catch (e) {
        mostrarAlertaError(e instanceof Error ? e.message : 'No se pudo anular la factura.');
    }
}

async function eliminarDesdeDetalle(): Promise<void> {
    if (facturaId == null || !facturaActual) return;
    if (!(await abrirConfirmacion(document.getElementById('dlg-eliminar') as HTMLDialogElement, obtenerIdentificadorFactura(facturaActual)))) return;
    try {
        await facturaService.eliminarFactura(facturaId);
        window.location.href = 'index.html';
    } catch (e) {
        mostrarAlertaError(e instanceof Error ? e.message : 'No se pudo eliminar la factura.');
    }
}

// ─── Event Wiring ───────────────────────────────────────────────────────────
function configurarEventos(): void {
    btnCerrarAlerta?.addEventListener('click', ocultarAlertaError);
    formFactura?.addEventListener('submit', (e) => {
        e.preventDefault();
        void guardar();
    });

    const obtenerUrlRetorno = (): string => {
        return (modoActual === 'editar' && facturaId != null)
            ? `nueva-factura.html?id=${facturaId}&modo=ver`
            : 'index.html';
    };

    btnCancelar?.addEventListener('click', (e) => {
        e.preventDefault();
        if (hayCambios()) {
            dlgDescartar.showModal();
        } else {
            window.location.href = obtenerUrlRetorno();
        }
    });

    linkVolver?.addEventListener('click', (e) => {
        if (hayCambios()) {
            e.preventDefault();
            dlgDescartar.showModal();
        }
    });

    dlgDescartar?.addEventListener('close', () => {
        if (dlgDescartar.returnValue === 'confirmar') {
            window.location.href = obtenerUrlRetorno();
        }
    });

    btnAbrirModalCliente?.addEventListener('click', () => modalCliente.abrir());
    clienteDisplay?.addEventListener('click', () => modalCliente.abrir());

    btnLimpiarCliente?.addEventListener('click', () => {
        inputClienteId.value = '';
        clienteDisplay.value = '';
        btnLimpiarCliente.hidden = true;
    });

    selectCondicionIva?.addEventListener('change', () => {
        const cf = esConsumidorFinal();
        clienteOpcional.hidden = !cf;
        hintCliente.textContent = cf ? 'Podés dejarlo sin cliente o elegir uno.' : '';
    });

    selectListaBase?.addEventListener('change', () => {
        listaBaseId = selectListaBase.value ? parseInt(selectListaBase.value, 10) : null;
    });

    btnEditar?.addEventListener('click', (e) => {
        if (facturaId != null) {
            e.preventDefault();
            window.location.href = `nueva-factura.html?id=${facturaId}&modo=editar`;
        }
    });

    btnMasAcciones?.addEventListener('click', (e) => {
        e.stopPropagation();
        menuFactura.hidden = !menuFactura.hidden;
    });

    menuFactura?.addEventListener('click', (e) => {
        const btn = (e.target as HTMLElement).closest<HTMLButtonElement>('[data-menu]');
        if (!btn) return;
        menuFactura.hidden = true;
        if (btn.dataset.menu === 'anular') void anularDesdeDetalle();
        if (btn.dataset.menu === 'eliminar') void eliminarDesdeDetalle();
    });

    // Informar a ARCA desde detalle
    btnInformar?.addEventListener('click', () => {
        if (!facturaId || !dlgInformar) return;
        dlgInformar.querySelectorAll<HTMLElement>('[data-campo="cantidad"]').forEach((el) => {
            el.textContent = '1';
        });
        dlgInformar.querySelectorAll<HTMLElement>('[data-campo="detalle"]').forEach((el) => {
            el.textContent = facturaActual ? obtenerIdentificadorFactura(facturaActual) : `#${facturaId}`;
        });
        dlgInformar.querySelectorAll<HTMLElement>('[data-fase]').forEach((el) => {
            el.hidden = el.getAttribute('data-fase') !== 'confirmar';
        });
        dlgInformar.showModal();
    });

    dlgInformar?.addEventListener('submit', async (e) => {
        const btn = (e.submitter as HTMLButtonElement | null)?.value;
        const form = e.target as HTMLFormElement;
        const fase = form.getAttribute('data-fase');

        if (fase === 'confirmar' && btn === 'confirmar' && facturaId != null) {
            e.preventDefault();
            dlgInformar.querySelectorAll<HTMLElement>('[data-fase]').forEach((el) => {
                el.hidden = el.getAttribute('data-fase') !== 'informando';
            });
            try {
                const res = await facturaService.informarArca([facturaId]);
                const chips = dlgInformar.querySelector('#informar-chips') as HTMLElement;
                const cuerpo = dlgInformar.querySelector('#informar-resultado-body') as HTMLTableSectionElement;
                const tplResultado = document.getElementById('tpl-resultado-fila') as HTMLTemplateElement;

                chips.replaceChildren();
                cuerpo.replaceChildren();
                for (const r of res) {
                    const clone = tplResultado.content.cloneNode(true) as DocumentFragment;
                    const fila = clone.querySelector('tr') as HTMLTableRowElement;
                    (fila.querySelector('[data-campo="factura"]') as HTMLElement).textContent = r.nroFactura != null ? `#${r.nroFactura}` : `#${r.id}`;
                    aplicarBadgeEstado(fila.querySelector('[data-campo="estado"]') as HTMLElement, r.estado);
                    (fila.querySelector('[data-campo="detalle"]') as HTMLElement).textContent = r.mensaje ?? '—';
                    (fila.querySelector('[data-campo="enlace"]') as HTMLAnchorElement).href = `nueva-factura.html?id=${r.id}&modo=ver`;
                    cuerpo.appendChild(fila);
                }
                dlgInformar.querySelectorAll<HTMLElement>('[data-fase]').forEach((el) => {
                    el.hidden = el.getAttribute('data-fase') !== 'resultado';
                });
            } catch {
                dlgInformar.querySelectorAll<HTMLElement>('[data-fase]').forEach((el) => {
                    el.hidden = el.getAttribute('data-fase') !== 'error';
                });
            }
        }
    });

    dlgInformar?.addEventListener('close', () => {
        if (facturaId != null) {
            void cargarFacturaExistente(facturaId);
        }
    });

    document.addEventListener('click', (e) => {
        if (!menuFactura.hidden && !menuFactura.contains(e.target as Node) && e.target !== btnMasAcciones) {
            menuFactura.hidden = true;
        }
        if (!menuDetalle.hidden && !menuDetalle.contains(e.target as Node)) {
            cerrarMenuDetalle();
        }
    });

    // Menú de opciones del detalle (cambiar lista o descripción)
    menuDetalle?.addEventListener('click', (e) => {
        const btn = (e.target as HTMLElement).closest<HTMLButtonElement>('[data-menu]');
        if (!btn || menuDetalleIdx == null) return;
        const accion = btn.dataset.menu;
        const idx = menuDetalleIdx;
        cerrarMenuDetalle();

        const detalle = tablaDetalles.obtenerDetalles()[idx];
        if (!detalle) return;

        if (accion === 'editar-lista') {
            facturaService.listasPrecioDeArticulo(detalle.articuloId).then((listas) => {
                editListaPrecioId.replaceChildren();
                for (const l of listas) {
                    const opt = new Option(`${l.listaPrecio} — ${formatearMoneda(l.precioVenta)}`, String(l.id));
                    opt.dataset.precio = String(l.precioVenta);
                    opt.dataset.denominacion = l.listaPrecio;
                    editListaPrecioId.appendChild(opt);
                }
                editListaPrecioId.value = String(detalle.listaPrecioArticuloId);
                detalleEditando = idx;
                dlgEditarLista.showModal();
            }).catch((err) => mostrarAlertaError(err instanceof Error ? err.message : 'Error al obtener listas'));
        } else if (accion === 'editar-descripcion') {
            editDescripcion.value = detalle.descripcion || '';
            detalleEditando = idx;
            dlgEditarDescripcion.showModal();
        }
    });

    dlgEditarLista?.addEventListener('close', () => {
        if (dlgEditarLista.returnValue === 'confirmar' && detalleEditando != null) {
            const opt = editListaPrecioId.selectedOptions[0];
            if (opt) {
                tablaDetalles.actualizarDetalle(detalleEditando, {
                    listaPrecioArticuloId: parseInt(editListaPrecioId.value, 10),
                    precioUnitario: parseFloat(opt.dataset.precio || '0'),
                    listaPrecioDenominacion: opt.dataset.denominacion || ''
                });
            }
        }
    });

    dlgEditarDescripcion?.addEventListener('close', () => {
        if (dlgEditarDescripcion.returnValue === 'confirmar' && detalleEditando != null) {
            tablaDetalles.actualizarDetalle(detalleEditando, {
                descripcion: editDescripcion.value.trim() || undefined
            });
        }
    });

    // Tecla Escape para modales y menús
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            if (!menuFactura.hidden) menuFactura.hidden = true;
            if (!menuDetalle.hidden) cerrarMenuDetalle();
        }
    });
}

// ─── Inicialización ─────────────────────────────────────────────────────────
async function init(): Promise<void> {
    const params = new URLSearchParams(window.location.search);
    const paramModo = params.get('modo') as ModoFactura | null;
    const paramId = params.get('id');

    if (paramId) {
        facturaId = parseInt(paramId, 10);
        modoActual = paramModo === 'editar' ? 'editar' : 'ver';
    } else {
        modoActual = 'nueva';
    }

    document.body.dataset.modo = modoActual;
    tablaDetalles.setModo(modoActual);

    if (modoActual === 'nueva') {
        tituloPagina.textContent = 'Nueva factura';
    } else if (modoActual === 'editar') {
        tituloPagina.textContent = 'Editar factura';
        if (facturaId != null && linkVolver) {
            linkVolver.href = `nueva-factura.html?id=${facturaId}&modo=ver`;
            linkVolver.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#i-back"/></svg> Volver a la factura`;
        }
    } else {
        tituloPagina.textContent = 'Factura';
    }

    configurarEventos();
    await cargarCatalogos();

    if (facturaId != null) {
        await cargarFacturaExistente(facturaId);
    } else {
        snapshotInicial = serializarEstado();
    }
}

void init();
