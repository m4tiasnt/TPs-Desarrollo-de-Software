import {
    FacturaRequestDTO,
    FacturaDetalleRequestDTO,
    FacturaReporteDTO,
    ListaPrecioDTO,
    ClienteDTO,
    PuntoVentaDTO,
    CondicionIvaDTO,
    TipoMonedaDTO,
    ArticuloSearchDTO,
    ListaPrecioArticuloDTO
} from './models/api.dto.js';

import { FacturaService } from './services/FacturaService.js';

// Instancia del servicio
const facturaService = new FacturaService('http://localhost:8080/api/facturas');

// Estado de la factura en memoria
interface RenglonUI extends Omit<FacturaDetalleRequestDTO, 'listaPrecioArticuloId' | 'articuloId'> {
    articuloDenominacion: string;
    articuloCodigo: string;
    precioUnitario: number;
    listaPrecioDenominacion: string;
    listaPrecioArticuloId: number; // requerido en UI
    articuloId: number; // requerido en UI
}

let renglones: RenglonUI[] = [];
let listaBaseId: number | null = null;
let listasDePrecioCache: ListaPrecioDTO[] = [];

// Elementos DOM
const formFactura = document.getElementById('form-factura') as HTMLFormElement;
const alertaError = document.getElementById('alerta-error') as HTMLDivElement;
const alertaErrorTexto = document.getElementById('alerta-error-texto') as HTMLSpanElement;
const btnCerrarAlerta = document.getElementById('btn-cerrar-alerta') as HTMLButtonElement;
const linkVolver = document.getElementById('link-volver') as HTMLAnchorElement;

// Cabecera
const selectCondicionIva = document.getElementById('condicionIvaId') as HTMLSelectElement;
const inputClienteId = document.getElementById('clienteId') as HTMLInputElement;
const clienteDisplay = document.getElementById('clienteDisplay') as HTMLInputElement;
const btnAbrirModalCliente = document.getElementById('btnAbrirModalCliente') as HTMLButtonElement;

// Elementos del Modal Explorer de Clientes
const modalClienteExplorer = document.getElementById('modal-cliente-explorer') as HTMLDivElement;
const btnCerrarModalCliente = document.getElementById('btnCerrarModalCliente') as HTMLButtonElement;
const inputBuscarCliente = document.getElementById('inputBuscarCliente') as HTMLInputElement;
const btnBuscarCliente = document.getElementById('btnBuscarCliente') as HTMLButtonElement;
const tbodyClientesExplorer = document.getElementById('tbody-clientes-explorer') as HTMLTableSectionElement;
const btnPaginacionAnterior = document.getElementById('btnPaginacionAnterior') as HTMLButtonElement;
const btnPaginacionSiguiente = document.getElementById('btnPaginacionSiguiente') as HTMLButtonElement;
const infoPaginacionCliente = document.getElementById('infoPaginacionCliente') as HTMLSpanElement;

let paginaActualClientes = 0;
let totalPaginasClientes = 0;
let terminoBusquedaCliente = '';
let selectedClienteId: number | null = null;
const clienteOpcional = document.getElementById('cliente-opcional') as HTMLSpanElement;
const hintCliente = document.getElementById('hint-cliente') as HTMLParagraphElement;
const errorCliente = document.getElementById('error-clienteId') as HTMLParagraphElement;
const inputFecha = document.getElementById('fechaEmision') as HTMLInputElement;
const selectPuntoVenta = document.getElementById('puntoVentaId') as HTMLSelectElement;
const selectTipoMoneda = document.getElementById('tipoMonedaId') as HTMLSelectElement;
const selectListaBase = document.getElementById('listaPrecioId') as HTMLSelectElement;
const textareaObservaciones = document.getElementById('observaciones') as HTMLTextAreaElement;

// Renglones
const buscadorArticulo = document.getElementById('buscador-articulo') as HTMLInputElement;
const listaArticulos = document.getElementById('lista-articulos') as HTMLUListElement;
const renglonesVacio = document.getElementById('renglones-vacio') as HTMLDivElement;
const renglonesWrap = document.getElementById('renglones-wrap') as HTMLDivElement;
const renglonesTbody = document.getElementById('renglones-tbody') as HTMLTableSectionElement;
const errorRenglones = document.getElementById('error-renglones') as HTMLParagraphElement;

// Resumen
const resumenSubtotal = document.getElementById('resumen-subtotal') as HTMLSpanElement;
const resumenBonif = document.getElementById('resumen-bonificaciones') as HTMLSpanElement;
const resumenTotal = document.getElementById('resumen-total') as HTMLSpanElement;

// Botones
const btnGuardar = document.getElementById('btn-guardar') as HTMLButtonElement;
const btnCancelar = document.getElementById('btn-cancelar') as HTMLButtonElement;

// Diálogos
const dlgDescartar = document.getElementById('dlg-descartar') as HTMLDialogElement;
const dlgCambiarLista = document.getElementById('dlg-cambiar-lista') as HTMLDialogElement;
const dlgCambiarListaCantidad = dlgCambiarLista.querySelector('[data-campo="cantidadRenglones"]') as HTMLSpanElement;

// Plantillas
const tplOpcionArticulo = document.getElementById('tpl-opcion-articulo') as HTMLTemplateElement;
const tplRenglon = document.getElementById('tpl-renglon') as HTMLTemplateElement;

// Modal editar lista
const dlgEditarLista = document.getElementById('dlg-editar-lista') as HTMLDialogElement;
const editListaPrecioId = document.getElementById('edit-listaPrecioId') as HTMLSelectElement;

// Modal editar descripción
const dlgEditarDescripcion = document.getElementById('dlg-editar-descripcion') as HTMLDialogElement;
const editDescripcion = document.getElementById('edit-descripcion') as HTMLInputElement;

// Menú de acciones del renglón (único, en coordenadas de viewport)
const menuRenglon = document.getElementById('menu-renglon') as HTMLUListElement;
const menuBtnEditarLista = menuRenglon.querySelector('[data-menu="editar-lista"]') as HTMLButtonElement;
const menuBtnEditarDescripcion = menuRenglon.querySelector('[data-menu="editar-descripcion"]') as HTMLButtonElement;

let renglonEditandoIdx: number | null = null;
let menuRenglonIdx: number | null = null;
let menuRenglonToggle: HTMLButtonElement | null = null;
let indiceActivoCombobox = -1;
let pendienteMarcarPrimera = false;

// Inicialización
document.addEventListener('DOMContentLoaded', async () => {
    inputFecha.valueAsDate = new Date();
    await cargarCatalogos();
    setupEventListeners();
    renderizarTodo();
});

// Cargar catálogos al inicio
async function cargarCatalogos(): Promise<void> {
    try {
        const [listas, puntos, ivas, monedas] = await Promise.all([
            facturaService.obtenerListasPrecio(),
            facturaService.obtenerPuntosVenta(),
            facturaService.obtenerCondicionesIva(),
            facturaService.obtenerTiposMoneda()
        ]);

        listasDePrecioCache = listas;
        poblarSelect(selectListaBase, listas, 'denominacion', true);

        poblarSelect(selectCondicionIva, ivas, 'denominacion');
        poblarSelect(selectPuntoVenta, puntos, 'descripcion');
        poblarSelect(selectTipoMoneda, monedas, 'denominacion');

    } catch (e: any) {
        mostrarAlertaError(`Error cargando catálogos: ${e.message}`);
    }
}

function poblarSelect<T>(sel: HTMLSelectElement, items: T[], labelKey: string, conPlaceholder = true, metaKey?: keyof T): void {
    sel.innerHTML = '';
    if (conPlaceholder) {
        const ph = document.createElement('option');
        ph.value = '';
        ph.textContent = 'Seleccionar…';
        sel.appendChild(ph);
    }
    for (const item of items) {
        const opt = document.createElement('option');
        opt.value = String((item as any).id);
        const meta = metaKey ? ` (${(item as any)[metaKey]})` : '';
        opt.textContent = `${(item as any)[labelKey]}${meta}`;
        sel.appendChild(opt);
    }
}

// Event listeners
function setupEventListeners(): void {
    // Cerrar alerta
    btnCerrarAlerta?.addEventListener('click', () => ocultarAlertaError());

    // Condición IVA -> cliente opcional si Consumidor Final (id 3)
    selectCondicionIva.addEventListener('change', () => {
        const esConsumidorFinal = selectCondicionIva.value === '3';
        clienteOpcional.hidden = !esConsumidorFinal;
        hintCliente.textContent = esConsumidorFinal
            ? 'Podés dejarlo sin cliente (se emite a Consumidor Final) o elegir uno.'
            : '';
        if (esConsumidorFinal) {
            selectedClienteId = null;
            if (inputClienteId) inputClienteId.value = '';
            if (clienteDisplay) clienteDisplay.value = '';
            errorCliente.hidden = true;
            if (clienteDisplay) clienteDisplay.classList.remove('is-invalid');
        }
        validarCliente();
    });

    // Validar cliente al cambiar
    // Modal Explorer de Clientes listeners
    btnAbrirModalCliente?.addEventListener('click', abrirModalCliente);
    btnCerrarModalCliente?.addEventListener('click', cerrarModalCliente);
    modalClienteExplorer?.addEventListener('click', (e) => {
        if (e.target === modalClienteExplorer) {
            cerrarModalCliente();
        }
    });

    btnBuscarCliente?.addEventListener('click', () => {
        buscarClientes(inputBuscarCliente.value.trim(), 0);
    });

    inputBuscarCliente?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            buscarClientes(inputBuscarCliente.value.trim(), 0);
        }
    });

    btnPaginacionAnterior?.addEventListener('click', () => {
        if (paginaActualClientes > 0) {
            buscarClientes(terminoBusquedaCliente, paginaActualClientes - 1);
        }
    });

    btnPaginacionSiguiente?.addEventListener('click', () => {
        if (paginaActualClientes < totalPaginasClientes - 1) {
            buscarClientes(terminoBusquedaCliente, paginaActualClientes + 1);
        }
    });

    // Lista base cambia -> si hay renglones, pedir confirmación
    selectListaBase.addEventListener('change', () => {
        const nuevoId = selectListaBase.value ? parseInt(selectListaBase.value, 10) : null;
        if (renglones.length > 0 && nuevoId !== listaBaseId) {
            dlgCambiarListaCantidad.textContent = String(renglones.length);
            dlgCambiarLista.showModal();
            // El resultado se maneja en el evento 'close' del dialog
        } else {
            listaBaseId = nuevoId;
        }
    });

    dlgCambiarLista.addEventListener('close', async () => {
        if (dlgCambiarLista.returnValue === 'confirmar') {
            const nuevoId = selectListaBase.value ? parseInt(selectListaBase.value, 10) : null;
            listaBaseId = nuevoId;
            await recalcularTodosRenglonesConNuevaListaBase();
        } else {
            selectListaBase.value = listaBaseId ? String(listaBaseId) : '';
        }
    });

    // Combobox artículos
    let debounceTimer: number;
    buscadorArticulo.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = window.setTimeout(() => buscarArticulos(buscadorArticulo.value.trim()), 200);
    });
    buscadorArticulo.addEventListener('focus', () => {
        if (buscadorArticulo.value.trim()) buscarArticulos(buscadorArticulo.value.trim());
    });
    buscadorArticulo.addEventListener('keydown', manejarTecladoCombobox);
    document.addEventListener('click', (e: MouseEvent) => {
        const target = e.target as Node;
        if (!buscadorArticulo.contains(target) && !listaArticulos.contains(target)) {
            cerrarListaArticulos();
        }
    });

    // Form submit
    formFactura.addEventListener('submit', handleSubmit);

    // Cancelar / Volver -> dialog descartar si hay datos
    const hayDatos = () => renglones.length > 0 || textareaObservaciones.value.trim() || !!selectedClienteId || (inputClienteId && !!inputClienteId.value) || selectPuntoVenta.value;
    btnCancelar.addEventListener('click', (e) => { e.preventDefault(); if (hayDatos()) dlgDescartar.showModal(); else window.location.href = 'index.html'; });
    linkVolver.addEventListener('click', (e) => { if (hayDatos()) { e.preventDefault(); dlgDescartar.showModal(); } });
    dlgDescartar.addEventListener('close', () => { if (dlgDescartar.returnValue === 'confirmar') window.location.href = 'index.html'; });

    // Cerrar alerta / menú con Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            ocultarAlertaError();
            cerrarMenuRenglon();
        }
    });

    // Cerrar el menú de renglón al hacer click fuera
    document.addEventListener('click', (e: MouseEvent) => {
        const target = e.target;
        const dentroDelMenu = target instanceof Node && menuRenglon.contains(target);
        const esBotonMenu = target instanceof Element && target.closest('[data-accion="menu"]') !== null;
        if (!dentroDelMenu && !esBotonMenu) cerrarMenuRenglon();
    });

    // El menú está anclado al botón: si cambia el layout, se cierra
    window.addEventListener('resize', cerrarMenuRenglon);
    window.addEventListener('scroll', cerrarMenuRenglon, true);

    // Acciones del menú de renglón
    menuRenglon.addEventListener('keydown', (e: KeyboardEvent) => {
        if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;

        e.preventDefault();
        const items = Array.from(menuRenglon.querySelectorAll<HTMLButtonElement>('.dropdown-item'));
        if (items.length === 0) return;

        const actual = items.indexOf(document.activeElement as HTMLButtonElement);
        const paso = e.key === 'ArrowDown' ? 1 : -1;
        let siguiente = actual === -1 ? 0 : actual + paso;
        if (siguiente < 0) siguiente = items.length - 1;
        if (siguiente >= items.length) siguiente = 0;

        items[siguiente].focus();
    });

    menuBtnEditarLista.addEventListener('click', () => {
        const idx = menuRenglonIdx;
        cerrarMenuRenglon();
        if (idx !== null) abrirModalEditarLista(idx);
    });

    menuBtnEditarDescripcion.addEventListener('click', () => {
        const idx = menuRenglonIdx;
        cerrarMenuRenglon();
        if (idx !== null) abrirModalEditarDescripcion(idx);
    });

    // Modal editar lista: el precio y la denominación viajan en el dataset de la opción
    dlgEditarLista.addEventListener('close', () => {
        const idx = renglonEditandoIdx;
        renglonEditandoIdx = null;

        if (dlgEditarLista.returnValue !== 'confirmar' || idx === null) return;

        const r = renglones[idx];
        const opt = editListaPrecioId.selectedOptions[0];
        if (!r || !opt || !opt.value) return;

        const precio = Number(opt.dataset.precio ?? '0');
        if (!Number.isFinite(precio)) return;

        r.listaPrecioArticuloId = Number(opt.value);
        r.precioUnitario = precio;
        r.listaPrecioDenominacion = opt.dataset.denominacion || '';
        renderizarTodo();
    });

    // Modal editar descripción
    dlgEditarDescripcion.addEventListener('close', () => {
        const idx = renglonEditandoIdx;
        renglonEditandoIdx = null;

        if (dlgEditarDescripcion.returnValue !== 'confirmar' || idx === null) return;

        const r = renglones[idx];
        if (!r) return;

        r.descripcion = editDescripcion.value.trim() || undefined;
        renderizarTodo();
    });
}

function validarCliente(): void {
    const esConsumidorFinal = selectCondicionIva.value === '3';
    const tieneCliente = !!(selectedClienteId || (inputClienteId && inputClienteId.value));
    if (!esConsumidorFinal && !tieneCliente) {
        errorCliente.hidden = false;
        if (clienteDisplay) clienteDisplay.classList.add('is-invalid');
    } else {
        errorCliente.hidden = true;
            if (clienteDisplay) clienteDisplay.classList.remove('is-invalid');
    }
}

// Navegación por teclado del combobox (↑ ↓ Enter Escape)
function manejarTecladoCombobox(e: KeyboardEvent): void {
    if (e.key === 'Escape') {
        if (!listaArticulos.hidden) {
            e.preventDefault();
            e.stopPropagation();
            cerrarListaArticulos();
        }
        return;
    }

    if (e.key === 'Enter') {
        const opciones = opcionesArticulos();
        if (!listaArticulos.hidden && opciones.length > 0) {
            e.preventDefault();
            const indice = indiceActivoCombobox >= 0 ? indiceActivoCombobox : 0;
            opciones[indice].click();
        }
        return;
    }

    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;

    e.preventDefault();
    const paso = e.key === 'ArrowDown' ? 1 : -1;

    if (listaArticulos.hidden) {
        const consulta = buscadorArticulo.value.trim();
        if (consulta) {
            pendienteMarcarPrimera = paso > 0;
            void buscarArticulos(consulta);
        }
        return;
    }

    const total = opcionesArticulos().length;
    if (total === 0) return;

    let siguiente = indiceActivoCombobox === -1
        ? (paso > 0 ? 0 : total - 1)
        : indiceActivoCombobox + paso;

    if (siguiente < 0) siguiente = total - 1;
    if (siguiente >= total) siguiente = 0;

    marcarOpcionActiva(siguiente);
}

function opcionesArticulos(): HTMLLIElement[] {
    return Array.from(listaArticulos.querySelectorAll<HTMLLIElement>('li[role="option"]'));
}

function marcarOpcionActiva(indice: number): void {
    const opciones = opcionesArticulos();
    if (opciones.length === 0) {
        indiceActivoCombobox = -1;
        buscadorArticulo.removeAttribute('aria-activedescendant');
        return;
    }

    indiceActivoCombobox = indice;
    opciones.forEach((li, i) => li.setAttribute('aria-selected', String(i === indice)));

    const activa = opciones[indice];
    buscadorArticulo.setAttribute('aria-activedescendant', activa.id);
    activa.scrollIntoView({ block: 'nearest' });
}

function cerrarListaArticulos(): void {
    listaArticulos.hidden = true;
    buscadorArticulo.setAttribute('aria-expanded', 'false');
    buscadorArticulo.removeAttribute('aria-activedescendant');
    indiceActivoCombobox = -1;
    pendienteMarcarPrimera = false;
}

// Buscar artículos para combobox
async function buscarArticulos(q: string): Promise<void> {
    if (!q) {
        cerrarListaArticulos();
        return;
    }
    try {
        const resultados = await facturaService.buscarArticulos(q);
        listaArticulos.innerHTML = '';
        indiceActivoCombobox = -1;
        buscadorArticulo.removeAttribute('aria-activedescendant');

        if (resultados.length === 0) {
            const li = document.createElement('li');
            li.textContent = 'Sin resultados';
            li.style.padding = '8px 12px';
            li.style.color = 'var(--color-text-muted)';
            listaArticulos.appendChild(li);
        } else {
            for (const art of resultados) {
                const clone = tplOpcionArticulo.content.cloneNode(true) as DocumentFragment;
                const li = clone.querySelector('li') as HTMLLIElement;
                li.id = `opcion-articulo-${art.id}`;
                li.setAttribute('aria-selected', 'false');
                li.dataset.id = String(art.id);
                li.dataset.listaPrecioArticuloId = String(art.listaPrecioArticuloId);
                li.dataset.precio = String(art.precioVenta);
                (clone.querySelector('[data-campo="denominacion"]') as HTMLElement).textContent = art.denominacion;
                (clone.querySelector('[data-campo="detalle"]') as HTMLElement).textContent = `${art.codigo} · ${art.marca}`;
                const precioSpan = clone.querySelector('[data-campo="precio"]') as HTMLElement;
                if (art.precioVenta > 0) {
                    precioSpan.textContent = formatearMoneda(art.precioVenta);
                } else {
                    precioSpan.innerHTML = '<span class="badge badge-pendiente">Sin precio en esta lista</span>';
                }
                li.addEventListener('click', () => seleccionarArticulo(art));
                listaArticulos.appendChild(li);
            }
        }

        listaArticulos.hidden = false;
        buscadorArticulo.setAttribute('aria-expanded', 'true');

        if (pendienteMarcarPrimera) {
            pendienteMarcarPrimera = false;
            if (resultados.length > 0) marcarOpcionActiva(0);
        }
    } catch (e: any) {
        pendienteMarcarPrimera = false;
        console.error(e);
        mostrarAlertaError(`Error buscando artículos: ${e.message}`);
    }
}

function seleccionarArticulo(art: ArticuloSearchDTO): void {
    cerrarListaArticulos();
    buscadorArticulo.value = '';

    // Buscar listas de precios de este artículo para poblar el select del renglón
    facturaService.listasPrecioDeArticulo(art.id).then(listas => {
        if (listas.length === 0) {
            mostrarAlertaError(`El artículo "${art.denominacion}" no tiene lista de precios cargada.`);
            return;
        }
        agregarRenglon(art, listas);
    }).catch(e => {
        console.error(e);
        mostrarAlertaError(`Error cargando listas del artículo: ${e.message}`);
    });
}

// Agregar renglón con listas del artículo
async function agregarRenglon(art: ArticuloSearchDTO, listas: ListaPrecioArticuloDTO[]): Promise<void> {
    // Determinar lista inicial: si hay listaBase y el artículo la tiene, usarla; si no, primera de sus listas
    let listaInicial: ListaPrecioArticuloDTO | null = null;
    if (listaBaseId) {
        listaInicial = listas.find(l => l.listaPrecioId === listaBaseId) || null;
    }
    if (!listaInicial && listas.length > 0) {
        listaInicial = listas[0];
    }

    const nuevo: RenglonUI = {
        articuloId: art.id,
        listaPrecioArticuloId: listaInicial?.id || 0,
        descripcion: undefined,
        cantidad: 1,
        porcentajeBonificacion: 0,
        precioUnitario: listaInicial?.precioVenta || 0,
        listaPrecioDenominacion: listaInicial?.listaPrecio || '',
        articuloDenominacion: art.denominacion,
        articuloCodigo: art.codigo
    };

    renglones.push(nuevo);
    renderizarTodo();
}

// Renderizado completo
function renderizarTodo(): void {
    renderRenglones();
    renderResumen();
}

function renderRenglones(): void {
    cerrarMenuRenglon();

    if (renglones.length === 0) {
        renglonesVacio.hidden = false;
        renglonesWrap.hidden = true;
        errorRenglones.hidden = true;
        return;
    }
    renglonesVacio.hidden = true;
    renglonesWrap.hidden = false;
    errorRenglones.hidden = true;

    renglonesTbody.innerHTML = '';
    renglones.forEach((r, idx) => {
        const clone = tplRenglon.content.cloneNode(true) as DocumentFragment;
        const tr = clone.querySelector('tr') as HTMLTableRowElement;

        // Artículo: nombre + línea secundaria con código y lista vigente
        (tr.querySelector('[data-campo="articuloDenominacion"]') as HTMLElement).textContent = r.articuloDenominacion;
        const meta = [r.articuloCodigo, r.listaPrecioDenominacion].filter(Boolean).join(' · ');
        (tr.querySelector('[data-campo="articuloMeta"]') as HTMLElement).textContent = meta || '—';

        const descEl = tr.querySelector('[data-campo="articuloDescripcion"]') as HTMLElement;
        if (r.descripcion) {
            descEl.textContent = r.descripcion;
            descEl.hidden = false;
        } else {
            descEl.textContent = '';
            descEl.hidden = true;
        }

        (tr.querySelector('[data-campo="articuloId"]') as HTMLInputElement).value = String(r.articuloId);
        (tr.querySelector('[data-campo="listaPrecioArticuloId"]') as HTMLInputElement).value = String(r.listaPrecioArticuloId);
        (tr.querySelector('[data-campo="importeSubtotal"]') as HTMLElement).textContent = formatearMoneda(calcularSubtotal(r));

        // Precio unitario (readonly)
        const inpPrecio = tr.querySelector('input[name="precioUnitario"]') as HTMLInputElement;
        inpPrecio.value = formatearMoneda(r.precioUnitario);

        // Inputs con eventos
        const inpCant = tr.querySelector('input[name="cantidad"]') as HTMLInputElement;
        inpCant.value = String(r.cantidad);
        inpCant.addEventListener('input', () => onCambioCantidad(idx, parseFloat(inpCant.value) || 1));

        const inpBonif = tr.querySelector('input[name="porcentajeBonificacion"]') as HTMLInputElement;
        inpBonif.value = String(r.porcentajeBonificacion ?? 0);
        inpBonif.addEventListener('input', () => onCambioBonif(idx, parseFloat(inpBonif.value) || 0));

        // Menú de acciones: un único menú global, anclado a este botón
        const btnMenu = tr.querySelector('[data-accion="menu"]') as HTMLButtonElement;
        btnMenu.addEventListener('click', (e) => {
            e.stopPropagation();
            alternarMenuRenglon(idx, btnMenu);
        });

        // Botón quitar
        const btnQuitar = tr.querySelector('[data-accion="quitar"]') as HTMLButtonElement;
        btnQuitar.addEventListener('click', () => quitarRenglon(idx));

        renglonesTbody.appendChild(tr);
    });
}

function alternarMenuRenglon(idx: number, toggle: HTMLButtonElement): void {
    if (menuRenglonIdx === idx && !menuRenglon.hidden) {
        cerrarMenuRenglon();
        return;
    }

    if (menuRenglonToggle) menuRenglonToggle.setAttribute('aria-expanded', 'false');

    menuRenglonIdx = idx;
    menuRenglonToggle = toggle;
    menuRenglon.hidden = false;
    posicionarMenuRenglon(toggle);
    toggle.setAttribute('aria-expanded', 'true');
    menuBtnEditarLista.focus();
}

function posicionarMenuRenglon(toggle: HTMLButtonElement): void {
    const btn = toggle.getBoundingClientRect();
    const menu = menuRenglon.getBoundingClientRect();
    const ancho = menu.width || 190;
    const alto = menu.height || 88;

    const izquierda = Math.max(8, Math.min(btn.right - ancho, window.innerWidth - ancho - 8));
    let arriba = btn.bottom + 4;
    if (arriba + alto > window.innerHeight - 8) {
        arriba = Math.max(8, btn.top - alto - 4);
    }

    menuRenglon.style.left = `${izquierda}px`;
    menuRenglon.style.top = `${arriba}px`;
}

function cerrarMenuRenglon(): void {
    if (menuRenglon.hidden) {
        menuRenglonIdx = null;
        return;
    }

    const teniaFoco = menuRenglon.contains(document.activeElement);

    menuRenglon.hidden = true;
    menuRenglonIdx = null;

    if (menuRenglonToggle) {
        menuRenglonToggle.setAttribute('aria-expanded', 'false');
        if (teniaFoco) menuRenglonToggle.focus();
        menuRenglonToggle = null;
    }
}

function abrirModalEditarLista(idx: number): void {
    const r = renglones[idx];
    if (!r) return;

    renglonEditandoIdx = idx;

    facturaService.listasPrecioDeArticulo(r.articuloId).then(listas => {
        if (listas.length === 0) {
            renglonEditandoIdx = null;
            mostrarAlertaError(`El artículo "${r.articuloDenominacion}" no tiene listas de precios cargadas.`);
            return;
        }

        editListaPrecioId.innerHTML = '';

        for (const l of listas) {
            const opt = document.createElement('option');
            opt.value = String(l.id);
            opt.dataset.precio = String(l.precioVenta);
            opt.dataset.denominacion = l.listaPrecio;
            opt.textContent = `${l.listaPrecio} — ${formatearMoneda(l.precioVenta)}`;
            editListaPrecioId.appendChild(opt);
        }

        editListaPrecioId.value = String(r.listaPrecioArticuloId);
        dlgEditarLista.showModal();
    }).catch(e => {
        renglonEditandoIdx = null;
        console.error(e);
        mostrarAlertaError(`Error cargando listas: ${e.message}`);
    });
}

function abrirModalEditarDescripcion(idx: number): void {
    const r = renglones[idx];
    if (!r) return;

    renglonEditandoIdx = idx;
    editDescripcion.value = r.descripcion || '';
    dlgEditarDescripcion.showModal();
}

function onCambioCantidad(idx: number, cant: number): void {
    renglones[idx].cantidad = Math.max(1, cant);
    actualizarSubtotalFila(idx);
    renderResumen();
}

function onCambioBonif(idx: number, bonif: number): void {
    renglones[idx].porcentajeBonificacion = Math.min(100, Math.max(0, bonif));
    actualizarSubtotalFila(idx);
    renderResumen();
}

// Actualiza solo la celda de subtotal para no perder el foco del input
function actualizarSubtotalFila(idx: number): void {
    const fila = renglonesTbody.querySelectorAll('tr')[idx];
    if (!fila) return;

    const celda = fila.querySelector('[data-campo="importeSubtotal"]');
    if (celda) celda.textContent = formatearMoneda(calcularSubtotal(renglones[idx]));
}

function quitarRenglon(idx: number): void {
    renglones.splice(idx, 1);
    renderizarTodo();
}

// Recalcular todos los renglones al cambiar lista base
async function recalcularTodosRenglonesConNuevaListaBase(): Promise<void> {
    const baseId = listaBaseId;
    if (baseId == null) return;
    for (let i = 0; i < renglones.length; i++) {
        const r = renglones[i];
        try {
            const listas = await facturaService.listasPrecioDeArticulo(r.articuloId);
            let encontrada: ListaPrecioArticuloDTO | undefined;
            for (const l of listas) {
                if (l.listaPrecioId === baseId) { encontrada = l; break; }
            }
            if (encontrada) {
                r.listaPrecioArticuloId = encontrada.id;
                r.precioUnitario = encontrada.precioVenta;
                r.listaPrecioDenominacion = encontrada.listaPrecio;
            }
        } catch {
            // Si falla, mantener actual
        }
    }
    renderizarTodo();
}

// Cálculos cliente-side
function calcularSubtotal(r: RenglonUI): number {
    const precioNeto = r.precioUnitario * (1 - (r.porcentajeBonificacion || 0) / 100);
    return Math.round(precioNeto * r.cantidad * 100) / 100;
}

function calcularTotales(): { subtotal: number; bonif: number; total: number } {
    let subtotal = 0, bonif = 0;
    for (const r of renglones) {
        const precioLista = r.precioUnitario * r.cantidad;
        const desc = precioLista * ((r.porcentajeBonificacion || 0) / 100);
        const neto = precioLista - desc;
        subtotal += precioLista;
        bonif += desc;
    }
    return { subtotal: Math.round(subtotal * 100) / 100, bonif: Math.round(bonif * 100) / 100, total: Math.round((subtotal - bonif) * 100) / 100 };
}

function renderResumen(): void {
    const { subtotal, bonif, total } = calcularTotales();
    resumenSubtotal.textContent = formatearMoneda(subtotal);
    resumenBonif.textContent = formatearMoneda(bonif);
    resumenTotal.textContent = formatearMoneda(total);
}

function formatearMoneda(v: number): string {
    return '$ ' + v.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Submit
async function handleSubmit(e: Event): Promise<void> {
    e.preventDefault();

    if (renglones.length === 0) {
        errorRenglones.hidden = false;
        return;
    }
    if (!selectCondicionIva.value) { mostrarAlertaError('Elegí una condición IVA'); return; }
    if (!selectPuntoVenta.value) { mostrarAlertaError('Elegí un punto de venta'); return; }
    if (!selectTipoMoneda.value) { mostrarAlertaError('Elegí un tipo de moneda'); return; }
    if (!selectListaBase.value) { mostrarAlertaError('Elegí una lista base'); return; }
    const idClienteFinal = selectedClienteId ?? (inputClienteId?.value ? parseInt(inputClienteId.value, 10) : undefined);
    if (!idClienteFinal && selectCondicionIva.value !== '3') {
        validarCliente(); return;
    }

    // Preparar DTO para backend
    const dto: FacturaRequestDTO = {
        clienteId: idClienteFinal,
        condicionIvaId: parseInt(selectCondicionIva.value, 10),
        puntoVentaId: parseInt(selectPuntoVenta.value, 10),
        tipoMonedaId: parseInt(selectTipoMoneda.value, 10),
        observaciones: textareaObservaciones.value.trim() || undefined,
        detalles: renglones.map(r => ({
            articuloId: r.articuloId,
            listaPrecioArticuloId: r.listaPrecioArticuloId,
            descripcion: r.descripcion,
            cantidad: r.cantidad,
            porcentajeBonificacion: r.porcentajeBonificacion
        }))
    };

    try {
        btnGuardar.disabled = true;
        btnGuardar.textContent = 'Guardando…';
        ocultarAlertaError();
        const creada = await facturaService.crearFactura(dto);
        mostrarMensaje(`¡Factura N° ${creada.numeroFactura} creada!`, 'exito');
        setTimeout(() => window.location.href = 'index.html', 1000);
    } catch (err: any) {
        mostrarAlertaError(err.message);
    } finally {
        btnGuardar.disabled = false;
        btnGuardar.textContent = 'Guardar factura';
    }
}

// UI helpers
function mostrarMensaje(texto: string, tipo: 'exito' | 'error'): void {
    const cont = document.getElementById('mensaje-feedback') as HTMLDivElement;
    if (!cont) return;
    cont.textContent = texto;
    cont.className = tipo;
    cont.style.display = 'block';
    setTimeout(() => cont.style.display = 'none', 5000);
}

function mostrarAlertaError(texto: string): void {
    alertaErrorTexto.textContent = texto;
    alertaError.hidden = false;
}

function ocultarAlertaError(): void {
    alertaError.hidden = true;
}
// ============================================================================
// LÓGICA DEL MODAL EXPLORER DE CLIENTES
// ============================================================================

async function buscarClientes(termino: string, pagina: number = 0): Promise<void> {
    try {
        tbodyClientesExplorer.innerHTML = '<tr><td colspan="3" style="text-align: center; padding: 15px; color: #64748b;">Cargando clientes…</td></tr>';
        
        const url = 'http://localhost:8080/api/clientes/buscar?filtro=' + encodeURIComponent(termino) + '&page=' + pagina + '&size=5';
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Error al obtener clientes: ' + response.statusText);
        }
        
        const data = await response.json();
        const lista: Array<{ id: number; cuitCuil: string; denominacion: string }> = data.content ?? data.contenido ?? [];
        totalPaginasClientes = data.totalPages ?? data.totalPaginas ?? 0;
        paginaActualClientes = data.number ?? data.paginaActual ?? 0;
        terminoBusquedaCliente = termino;

        renderizarTablaClientes(lista);
        actualizarPaginacionCliente();
    } catch (err: any) {
        console.error('Error al buscar clientes:', err);
        tbodyClientesExplorer.innerHTML = '<tr><td colspan="3" style="text-align: center; color: #dc2626; padding: 15px;">Error al obtener clientes del servidor.</td></tr>';
    }
}

function renderizarTablaClientes(clientes: Array<{ id: number; cuitCuil: string; denominacion: string }>): void {
    tbodyClientesExplorer.innerHTML = '';

    if (clientes.length === 0) {
        tbodyClientesExplorer.innerHTML = '<tr><td colspan="3" style="text-align: center; padding: 15px; color: #64748b;">No se encontraron clientes para la búsqueda.</td></tr>';
        return;
    }

    clientes.forEach(cliente => {
        const tr = document.createElement('tr');
        tr.style.borderBottom = '1px solid #f1f5f9';

        const tdCuit = document.createElement('td');
        tdCuit.style.padding = '8px 10px';
        tdCuit.textContent = cliente.cuitCuil;

        const tdDenominacion = document.createElement('td');
        tdDenominacion.style.padding = '8px 10px';
        tdDenominacion.textContent = cliente.denominacion;

        const tdAccion = document.createElement('td');
        tdAccion.style.padding = '8px 10px';
        tdAccion.style.textAlign = 'center';

        const btnSelect = document.createElement('button');
        btnSelect.type = 'button';
        btnSelect.className = 'btn btn-primary';
        btnSelect.style.padding = '4px 10px';
        btnSelect.style.fontSize = '0.85rem';
        btnSelect.textContent = 'Seleccionar';
        btnSelect.addEventListener('click', () => {
            seleccionarCliente(cliente);
        });

        tdAccion.appendChild(btnSelect);
        tr.appendChild(tdCuit);
        tr.appendChild(tdDenominacion);
        tr.appendChild(tdAccion);
        tbodyClientesExplorer.appendChild(tr);
    });
}

function actualizarPaginacionCliente(): void {
    if (totalPaginasClientes === 0) {
        infoPaginacionCliente.textContent = 'Página 0 de 0';
        btnPaginacionAnterior.disabled = true;
        btnPaginacionSiguiente.disabled = true;
    } else {
        infoPaginacionCliente.textContent = 'Página ' + (paginaActualClientes + 1) + ' de ' + totalPaginasClientes;
        btnPaginacionAnterior.disabled = paginaActualClientes === 0;
        btnPaginacionSiguiente.disabled = paginaActualClientes >= totalPaginasClientes - 1;
    }
}

function seleccionarCliente(cliente: { id: number; cuitCuil: string; denominacion: string }): void {
    selectedClienteId = cliente.id;
    if (inputClienteId) {
        inputClienteId.value = String(cliente.id);
    }
    if (clienteDisplay) {
        clienteDisplay.value = cliente.denominacion + ' (' + cliente.cuitCuil + ')';
    }
    validarCliente();
    cerrarModalCliente();
}

function abrirModalCliente(): void {
    if (modalClienteExplorer) {
        modalClienteExplorer.style.display = 'flex';
        inputBuscarCliente.focus();
        buscarClientes(inputBuscarCliente.value.trim(), 0);
    }
}

function cerrarModalCliente(): void {
    if (modalClienteExplorer) {
        modalClienteExplorer.style.display = 'none';
    }
}
