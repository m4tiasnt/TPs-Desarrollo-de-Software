import {
    ClienteDTO,
    EstadoFactura,
    FacturaFiltrosDTO,
    FacturaVentaListadoDTO,
    ResultadoAutorizacionDTO
} from './models/api.dto';
import { FacturaService } from './services/FacturaService';
import {
    aplicarBadgeEstado,
    esFacturaInformable,
    formatearFecha,
    formatearMoneda,
    obtenerIdentificadorFactura
} from './utils/formatters';
import { PaginadorControl } from './components/PaginadorControl';
import { NotificadorToast } from './components/NotificadorToast';
import { BuscadorClienteModal } from './components/BuscadorClienteModal';

// ─── Servicios e instancias compartidas ─────────────────────────────────────
const facturaService = new FacturaService('http://localhost:8080/api/facturas');
const toast = new NotificadorToast('mensaje-feedback');

// ─── Estado de la vista ─────────────────────────────────────────────────────
let paginaActual = 0;
let tamanioPagina = 20;
let modoSeleccion = false;
let tokenCarga = 0;

const seleccionados = new Set<number>();
const facturasPorId = new Map<number, FacturaVentaListadoDTO>();

// ─── Referencias DOM ────────────────────────────────────────────────────────
const formFiltros = document.getElementById('form-filtros') as HTMLFormElement | null;
const btnFiltros = document.getElementById('btn-filtros') as HTMLButtonElement | null;
const filtrosBadge = document.getElementById('filtros-badge') as HTMLElement | null;
const inputBusqueda = document.getElementById('filtro-busqueda') as HTMLInputElement | null;
const inputFechaDesde = document.getElementById('filtro-fecha-desde') as HTMLInputElement | null;
const inputFechaHasta = document.getElementById('filtro-fecha-hasta') as HTMLInputElement | null;
const selectEstado = document.getElementById('filtro-estado') as HTMLSelectElement | null;
const inputMontoMinimo = document.getElementById('filtro-monto-minimo') as HTMLInputElement | null;
const selectArca = document.getElementById('filtro-arca') as HTMLSelectElement | null;
const inputClienteId = document.getElementById('filtro-cliente-id') as HTMLInputElement | null;
const inputClienteNombre = document.getElementById('filtro-cliente-nombre') as HTMLInputElement | null;
const btnLimpiarCliente = document.getElementById('btn-limpiar-cliente') as HTMLButtonElement | null;

const tabla = document.getElementById('tabla-facturas') as HTMLTableElement | null;
const tbody = document.getElementById('tabla-facturas-body') as HTMLTableSectionElement | null;
const wrapTabla = document.getElementById('listado-tabla-wrap') as HTMLElement | null;
const contenedorMensaje = document.getElementById('listado-mensaje') as HTMLElement | null;
const chkSeleccionarTodos = document.getElementById('chk-seleccionar-todos') as HTMLInputElement | null;
const tplFila = document.getElementById('tpl-fila-factura') as HTMLTemplateElement;

const btnModoInformar = document.getElementById('btn-modo-informar') as HTMLButtonElement | null;
const btnExportarToggle = document.getElementById('btn-exportar-toggle') as HTMLButtonElement | null;
const menuExportar = document.getElementById('menu-exportar') as HTMLUListElement | null;
const btnExportarExcel = document.getElementById('btn-exportar-excel') as HTMLButtonElement | null;
const btnExportarPdf = document.getElementById('btn-exportar-pdf') as HTMLButtonElement | null;
const barraSeleccion = document.getElementById('barra-seleccion') as HTMLElement | null;
const seleccionInfo = document.getElementById('seleccion-info') as HTMLElement | null;
const btnCancelarSeleccion = document.getElementById('btn-cancelar-seleccion') as HTMLButtonElement | null;
const btnInformar = document.getElementById('btn-informar-arca') as HTMLButtonElement | null;

const menuFactura = document.getElementById('menu-factura') as HTMLUListElement | null;
let menuFacturaId: number | null = null;
let menuToggle: HTMLButtonElement | null = null;

const dlgInformar = document.getElementById('dlg-informar-arca') as HTMLDialogElement | null;
let informeEjecutado = false;

// ─── Modal de clientes ──────────────────────────────────────────────────────
const modalCliente = new BuscadorClienteModal({
    facturaService,
    onSeleccionarCliente: (cliente: ClienteDTO) => {
        if (inputClienteId) inputClienteId.value = String(cliente.id);
        if (inputClienteNombre) inputClienteNombre.value = cliente.denominacion;
        if (btnLimpiarCliente) btnLimpiarCliente.hidden = false;
        actualizarBadgeFiltros();
        paginaActual = 0;
        void cargarFacturas();
    }
});

function scrollearAlInicioDeTabla(): void {
    const contenedor = document.getElementById('listado-tabla-wrap') || tabla;
    if (!contenedor) return;
    const rect = contenedor.getBoundingClientRect();
    const topDeseado = window.scrollY + rect.top - 80;
    // Si la pantalla no está posicionada al inicio de la tabla (ej. usuario al pie paginando),
    // desplazamos suavemente hacia la cabecera.
    if (Math.abs(window.scrollY - topDeseado) > 30) {
        window.scrollTo({
            top: Math.max(0, topDeseado),
            behavior: 'smooth'
        });
    }
}

// ─── Control de Paginación ──────────────────────────────────────────────────
const paginador = new PaginadorControl({
    infoEl: document.getElementById('pag-info'),
    prevBtn: document.getElementById('pag-prev') as HTMLButtonElement | null,
    nextBtn: document.getElementById('pag-next') as HTMLButtonElement | null,
    tamanoSelect: document.getElementById('pag-tamano') as HTMLSelectElement | null,
    onCambiarPagina: (nuevaPagina) => {
        paginaActual = nuevaPagina;
        scrollearAlInicioDeTabla();
        void cargarFacturas();
    },
    onCambiarTamano: (nuevoTamano) => {
        tamanioPagina = nuevoTamano;
        paginaActual = 0;
        scrollearAlInicioDeTabla();
        void cargarFacturas();
    }
});

// ─── Filtros ────────────────────────────────────────────────────────────────
function filtrosActuales(): FacturaFiltrosDTO {
    const arca = selectArca?.value;
    const clienteIdVal = inputClienteId?.value ? parseInt(inputClienteId.value, 10) : undefined;
    const montoMinimoVal = inputMontoMinimo?.value ? parseFloat(inputMontoMinimo.value) : undefined;
    const busquedaVal = inputBusqueda?.value?.trim() || undefined;

    return {
        fechaDesde: inputFechaDesde?.value?.trim() || undefined,
        fechaHasta: inputFechaHasta?.value?.trim() || undefined,
        estado: (selectEstado?.value || undefined) as EstadoFactura | undefined,
        montoMinimo: montoMinimoVal !== undefined && !isNaN(montoMinimoVal) ? montoMinimoVal : undefined,
        clienteId: clienteIdVal !== undefined && !isNaN(clienteIdVal) ? clienteIdVal : undefined,
        busqueda: busquedaVal,
        informadaArca: arca === 'true' ? true : arca === 'false' ? false : undefined
    };
}

function contarFiltrosActivos(): number {
    const f = filtrosActuales();
    let cuenta = 0;
    if (f.busqueda) cuenta++;
    if (f.clienteId) cuenta++;
    if (f.fechaDesde) cuenta++;
    if (f.fechaHasta) cuenta++;
    if (f.estado) cuenta++;
    if (f.montoMinimo !== undefined) cuenta++;
    if (f.informadaArca !== undefined) cuenta++;
    return cuenta;
}

function actualizarBadgeFiltros(): void {
    const cuenta = contarFiltrosActivos();
    if (filtrosBadge) {
        filtrosBadge.textContent = String(cuenta);
        filtrosBadge.hidden = cuenta === 0;
    }
}

function limpiarFiltros(): void {
    formFiltros?.reset();
    if (inputClienteId) inputClienteId.value = '';
    if (inputClienteNombre) inputClienteNombre.value = '';
    if (btnLimpiarCliente) btnLimpiarCliente.hidden = true;
    actualizarBadgeFiltros();
    paginaActual = 0;
    void cargarFacturas();
}

// ─── Carga de datos ─────────────────────────────────────────────────────────
async function cargarFacturas(): Promise<void> {
    if (!tbody) return;
    cerrarMenuFila();
    const token = ++tokenCarga;
    mostrarCargando();
    paginador.bloquear(true);

    const filtros: FacturaFiltrosDTO = {
        ...filtrosActuales(),
        pagina: paginaActual,
        tamanio: tamanioPagina
    };

    try {
        const respuesta = await facturaService.obtenerFacturas(filtros);
        if (token !== tokenCarga) return;

        // Si se eliminó la última fila y la página quedó vacía
        if (respuesta.contenido.length === 0 && respuesta.totalElementos > 0 && paginaActual >= respuesta.totalPaginas) {
            paginaActual = Math.max(0, respuesta.totalPaginas - 1);
            await cargarFacturas();
            return;
        }

        if (respuesta.totalElementos === 0) {
            limpiarSeleccion();
            paginador.limpiar('Sin facturas');
            mostrarEstado(contarFiltrosActivos() > 0 ? 'tpl-sin-resultados' : 'tpl-sin-facturas');
            return;
        }

        ocultarEstado();
        renderizarTabla(respuesta.contenido);
        paginador.actualizar(respuesta);
        actualizarSeleccion();
    } catch (error) {
        if (token !== tokenCarga) return;
        const detalle = error instanceof Error ? error.message : 'Error de conexión con la API.';
        paginador.limpiar('No hay facturas');
        mostrarEstado('tpl-error-carga', detalle);
        toast.error('No se pudieron obtener las facturas del servidor.');
    } finally {
        if (token === tokenCarga) {
            paginador.bloquear(false);
            terminarCargaVisual();
        }
    }
}

function mostrarCargando(): void {
    if (contenedorMensaje) {
        contenedorMensaje.hidden = true;
        contenedorMensaje.replaceChildren();
    }
    if (wrapTabla) wrapTabla.hidden = false;
    if (!tbody) return;

    renderSkeletonFilas(Math.min(tamanioPagina, 8));
}

function renderSkeletonFilas(cantidad: number): void {
    if (!tbody) return;
    const fragment = document.createDocumentFragment();
    for (let i = 0; i < cantidad; i++) {
        const fila = document.createElement('tr');
        fila.className = 'fila-skeleton';
        fila.innerHTML = `
            <td class="col-select"><span class="skeleton" style="width:16px;height:16px;margin:auto;display:block;"></span></td>
            <td class="col-numero"><span class="skeleton" style="width:125px;height:14px;display:inline-block;"></span></td>
            <td class="col-fecha"><span class="skeleton" style="width:75px;height:14px;display:inline-block;"></span></td>
            <td class="col-cliente"><span class="skeleton" style="width:70%;height:14px;display:inline-block;"></span></td>
            <td class="col-total num"><span class="skeleton" style="width:85px;height:14px;margin-left:auto;display:block;"></span></td>
            <td class="col-estado"><span class="skeleton" style="width:80px;height:22px;border-radius:999px;display:inline-block;"></span></td>
            <td class="col-acciones actions"><span class="skeleton" style="width:28px;height:28px;border-radius:4px;margin-left:auto;display:block;"></span></td>
        `;
        fragment.appendChild(fila);
    }
    tbody.replaceChildren(fragment);
}

function terminarCargaVisual(): void {
    tabla?.classList.remove('is-loading');
    tabla?.removeAttribute('aria-busy');
}

function mostrarEstado(tplId: string, detalle?: string): void {
    terminarCargaVisual();
    const tpl = document.getElementById(tplId) as HTMLTemplateElement | null;
    if (!contenedorMensaje || !tpl || !wrapTabla) return;
    wrapTabla.hidden = true;
    contenedorMensaje.hidden = false;
    contenedorMensaje.replaceChildren(tpl.content.cloneNode(true));
    if (detalle) {
        const el = contenedorMensaje.querySelector('[data-campo="detalle"]');
        if (el) el.textContent = detalle;
    }
    contenedorMensaje.querySelector('[data-accion="reintentar"]')?.addEventListener('click', () => void cargarFacturas());
    contenedorMensaje.querySelector('[data-accion="limpiar-filtros"]')?.addEventListener('click', () => limpiarFiltros());
}

function ocultarEstado(): void {
    if (contenedorMensaje) {
        contenedorMensaje.hidden = true;
        contenedorMensaje.replaceChildren();
    }
    if (wrapTabla) wrapTabla.hidden = false;
}

// ─── Renderizado de tabla y selección por fila ──────────────────────────────
function renderizarTabla(facturas: FacturaVentaListadoDTO[]): void {
    if (!tbody) return;
    tbody.classList.remove('fade-in');
    facturasPorId.clear();

    const fragment = document.createDocumentFragment();

    for (const f of facturas) {
        facturasPorId.set(f.id, f);
        const clone = tplFila.content.cloneNode(true) as DocumentFragment;
        const fila = clone.querySelector('tr') as HTMLTableRowElement;
        fila.dataset.id = String(f.id);
        fila.dataset.estado = f.estado;

        const informable = esFacturaInformable(f.estado);
        if (!informable) {
            fila.classList.add('no-informable');
        }

        const nroEl = fila.querySelector('[data-campo="numero"]') as HTMLElement;
        if (f.numeroComprobante) {
            nroEl.textContent = f.numeroComprobante;
            nroEl.className = 'nro-factura';
        } else {
            nroEl.textContent = obtenerIdentificadorFactura(f);
            nroEl.className = 'nro-factura--provisorio';
        }

        const pvNumero = f.puntoVentaNumero != null ? String(f.puntoVentaNumero).padStart(4, '0') : '';
        const pvTexto = [pvNumero, f.puntoVentaDescripcion].filter(Boolean).join(' · ');
        setTextoCelda(fila, 'puntoVenta', pvTexto);
        setTextoCelda(fila, 'fechaEmision', formatearFecha(f.fechaEmision));
        const celdaCliente = fila.querySelector('[data-campo="cliente"]');
        if (celdaCliente) {
            const denom = f.clienteDenominacion?.trim();
            if (denom && denom !== 'Consumidor Final') {
                celdaCliente.textContent = denom;
            } else {
                celdaCliente.innerHTML = '<span class="muted">Cliente no registrado</span>';
            }
        }
        setTextoCelda(fila, 'condicionIva', f.condicionIva);
        setTextoCelda(fila, 'importeTotal', formatearMoneda(f.importeTotal));
        setTextoCelda(fila, 'cantidadItems', `${f.cantidadItems} ${f.cantidadItems === 1 ? 'detalle' : 'detalles'}`);

        aplicarBadgeEstado(fila.querySelector('[data-campo="estado"]') as HTMLElement, f.estado);

        const ver = fila.querySelector('[data-accion="ver"]') as HTMLAnchorElement;
        ver.href = `factura.html?id=${f.id}&modo=ver`;
        ver.setAttribute('aria-label', `Ver detalle de ${obtenerIdentificadorFactura(f)}`);

        const chk = fila.querySelector('input[name="facturaIds"]') as HTMLInputElement;
        chk.value = String(f.id);
        chk.id = `chk-factura-${f.id}`;
        chk.disabled = !informable;
        chk.checked = seleccionados.has(f.id);
        fila.classList.toggle('is-selected', chk.checked);

        // Toggle al cambiar el checkbox
        chk.addEventListener('change', () => {
            toggleSeleccionItem(f.id, chk.checked, fila);
        });

        // Usabilidad: seleccionar haciendo clic en cualquier parte de la fila
        fila.addEventListener('click', (e) => {
            if (!modoSeleccion || !informable) return;
            const target = e.target as HTMLElement;
            // No alternar si se hizo clic en un botón, enlace o dropdown
            if (target.closest('a, button, .dropdown-menu, input[type="checkbox"]')) return;

            chk.checked = !chk.checked;
            toggleSeleccionItem(f.id, chk.checked, fila);
        });

        const btnMenu = fila.querySelector('[data-accion="menu"]') as HTMLButtonElement;
        btnMenu.addEventListener('click', (e) => {
            e.stopPropagation();
            abrirMenuFila(f.id, btnMenu);
        });

        fragment.appendChild(fila);
    }

    tbody.replaceChildren(fragment);
    void tbody.offsetWidth;
    tbody.classList.add('fade-in');
}

function setTextoCelda(fila: HTMLElement, campo: string, texto: string): void {
    const el = fila.querySelector(`[data-campo="${campo}"]`);
    if (el) el.textContent = texto;
}

function toggleSeleccionItem(id: number, seleccionada: boolean, fila: HTMLElement): void {
    if (seleccionada) {
        seleccionados.add(id);
    } else {
        seleccionados.delete(id);
    }
    fila.classList.toggle('is-selected', seleccionada);
    actualizarSeleccion();
}

// ─── Modo selección / Informar a ARCA ───────────────────────────────────────
function entrarModoSeleccion(): void {
    modoSeleccion = true;
    document.body.classList.add('is-selecting');
    tabla?.classList.add('is-selecting');
    if (btnModoInformar) {
        btnModoInformar.classList.add('btn--active');
        btnModoInformar.setAttribute('aria-pressed', 'true');
        btnModoInformar.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#i-close"/></svg> Cancelar selección`;
    }
    if (barraSeleccion) barraSeleccion.hidden = false;
    actualizarSeleccion();
}

function salirModoSeleccion(): void {
    modoSeleccion = false;
    document.body.classList.remove('is-selecting');
    tabla?.classList.remove('is-selecting');
    if (btnModoInformar) {
        btnModoInformar.classList.remove('btn--active');
        btnModoInformar.setAttribute('aria-pressed', 'false');
        btnModoInformar.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#i-send"/></svg> Informar a ARCA`;
    }
    if (barraSeleccion) barraSeleccion.hidden = true;
    limpiarSeleccion();
}

function limpiarSeleccion(): void {
    seleccionados.clear();
    tbody?.querySelectorAll<HTMLTableRowElement>('tr').forEach((tr) => {
        tr.classList.remove('is-selected');
        const chk = tr.querySelector<HTMLInputElement>('input[name="facturaIds"]');
        if (chk) chk.checked = false;
    });
    if (chkSeleccionarTodos) chkSeleccionarTodos.checked = false;
    actualizarSeleccion();
}

function actualizarSeleccion(): void {
    if (!seleccionInfo) return;
    const n = seleccionados.size;

    if (n === 0) {
        seleccionInfo.innerHTML = 'Elegí las facturas a informar';
        if (btnInformar) btnInformar.disabled = true;
        return;
    }

    let sumaTotal = 0;
    for (const id of seleccionados) {
        const f = facturasPorId.get(id);
        if (f) sumaTotal += f.importeTotal;
    }

    const plural = n === 1 ? 'factura seleccionada' : 'facturas seleccionadas';
    seleccionInfo.innerHTML = `<strong>${n}</strong> ${plural} · Total: <span class="barra-seleccion-monto">${formatearMoneda(sumaTotal)}</span>`;

    if (btnInformar) btnInformar.disabled = false;

    // Actualizar checkbox de "seleccionar todos"
    if (chkSeleccionarTodos && tbody) {
        const habilitados = tbody.querySelectorAll<HTMLInputElement>('input[name="facturaIds"]:not(:disabled)');
        const marcados = tbody.querySelectorAll<HTMLInputElement>('input[name="facturaIds"]:checked');
        chkSeleccionarTodos.checked = habilitados.length > 0 && habilitados.length === marcados.length;
    }
}

// ─── Menú de acciones por fila ──────────────────────────────────────────────
function abrirMenuFila(id: number, toggle: HTMLButtonElement): void {
    if (!menuFactura) return;
    const factura = facturasPorId.get(id);
    if (!factura) return;

    if (menuFacturaId === id && !menuFactura.hidden) {
        cerrarMenuFila();
        return;
    }

    menuToggle?.setAttribute('aria-expanded', 'false');
    menuFacturaId = id;
    menuToggle = toggle;

    const puedeInformar = esFacturaInformable(factura.estado);
    const puedeEditar = factura.estado === 'BORRADOR' || factura.estado === 'RECHAZADA';
    const puedeAnular = puedeEditar;
    const puedeEliminar = factura.estado === 'BORRADOR';

    const btnInformarFila = menuFactura.querySelector('[data-menu="informar"]') as HTMLButtonElement | null;
    if (btnInformarFila) btnInformarFila.disabled = !puedeInformar;
    (menuFactura.querySelector('[data-menu="editar"]') as HTMLButtonElement).disabled = !puedeEditar;
    (menuFactura.querySelector('[data-menu="anular"]') as HTMLButtonElement).disabled = !puedeAnular;
    (menuFactura.querySelector('[data-menu="eliminar"]') as HTMLButtonElement).disabled = !puedeEliminar;

    menuFactura.hidden = false;
    posicionarMenu(toggle);
    toggle.setAttribute('aria-expanded', 'true');
}

function posicionarMenu(toggle: HTMLButtonElement): void {
    if (!menuFactura) return;
    const btn = toggle.getBoundingClientRect();
    const ancho = 190;
    const alto = 165;
    const izquierda = Math.max(8, Math.min(btn.right - ancho, window.innerWidth - ancho - 8));
    let arriba = btn.bottom + 4;
    if (arriba + alto > window.innerHeight - 8) {
        arriba = Math.max(8, btn.top - alto - 4);
    }
    menuFactura.style.left = `${izquierda}px`;
    menuFactura.style.top = `${arriba}px`;
}

function cerrarMenuFila(): void {
    if (!menuFactura || menuFactura.hidden) {
        menuFacturaId = null;
        return;
    }
    menuFactura.hidden = true;
    menuFacturaId = null;
    if (menuToggle) {
        menuToggle.setAttribute('aria-expanded', 'false');
        menuToggle = null;
    }
}

// ─── Control de Scroll del Body al abrir modales ────────────────────────────
function bloquearScrollBody(bloquear: boolean): void {
    if (bloquear) {
        document.documentElement.classList.add('modal-open');
        document.body.classList.add('modal-open');
    } else {
        document.documentElement.classList.remove('modal-open');
        document.body.classList.remove('modal-open');
    }
}

// ─── Diálogos de Confirmación (Anular / Eliminar) ────────────────────────────
function abrirConfirmacion(dlg: HTMLDialogElement, identificadorTexto: string): Promise<boolean> {
    dlg.querySelectorAll('[data-campo="identificador"]').forEach((el) => {
        el.textContent = identificadorTexto;
    });
    return new Promise((resolve) => {
        const alCerrar = () => {
            dlg.removeEventListener('close', alCerrar);
            bloquearScrollBody(false);
            resolve(dlg.returnValue === 'confirmar');
        };
        dlg.addEventListener('close', alCerrar);
        dlg.returnValue = 'cancelar';
        bloquearScrollBody(true);
        dlg.showModal();
    });
}

async function anularFactura(id: number): Promise<void> {
    const f = facturasPorId.get(id);
    if (!f) return;
    const dlg = document.getElementById('dlg-anular') as HTMLDialogElement;
    if (!(await abrirConfirmacion(dlg, obtenerIdentificadorFactura(f)))) return;

    try {
        await facturaService.anularFactura(id);
        toast.exito(`Factura ${obtenerIdentificadorFactura(f)} anulada con éxito.`);
        await cargarFacturas();
    } catch (error) {
        toast.error(error instanceof Error ? error.message : 'No se pudo anular la factura.');
    }
}

async function eliminarFactura(id: number): Promise<void> {
    const f = facturasPorId.get(id);
    if (!f) return;
    const dlg = document.getElementById('dlg-eliminar') as HTMLDialogElement;
    if (!(await abrirConfirmacion(dlg, obtenerIdentificadorFactura(f)))) return;

    try {
        await facturaService.eliminarFactura(id);
        toast.exito(`Factura ${obtenerIdentificadorFactura(f)} eliminada.`);
        await cargarFacturas();
    } catch (error) {
        toast.error(error instanceof Error ? error.message : 'No se pudo eliminar la factura.');
    }
}

// ─── Proceso de Informar a ARCA ─────────────────────────────────────────────
function mostrarFaseArca(fase: string): void {
    if (!dlgInformar) return;
    dlgInformar.querySelectorAll<HTMLElement>('[data-fase]').forEach((el) => {
        el.hidden = el.getAttribute('data-fase') !== fase;
    });
}

function abrirInformarArca(idEspecifico?: number): void {
    if (idEspecifico != null) {
        seleccionados.clear();
        seleccionados.add(idEspecifico);
        actualizarSeleccion();
    }
    if (!dlgInformar || seleccionados.size === 0) return;
    const ids = [...seleccionados];
    const facturas = ids.map((id) => facturasPorId.get(id)).filter((f): f is FacturaVentaListadoDTO => Boolean(f));
    const tbodyConfirmar = dlgInformar.querySelector('#informar-confirmar-body');
    const totalConfirmar = dlgInformar.querySelector('#informar-confirmar-total');
    if (tbodyConfirmar) {
        tbodyConfirmar.innerHTML = '';
        let suma = 0;
        for (const f of facturas) {
            suma += f.importeTotal;
            const tr = document.createElement('tr');
            const identificador = obtenerIdentificadorFactura(f);
            const ptoVta = `${String(f.puntoVentaNumero || 1).padStart(4, '0')}${f.puntoVentaDescripcion ? ' - ' + f.puntoVentaDescripcion : ''}`;
            const cliente = f.clienteDenominacion || 'Consumidor Final';
            tr.innerHTML = `
                <td><strong>${identificador}</strong></td>
                <td>${ptoVta}</td>
                <td><span>${cliente}</span></td>
                <td class="num">${formatearMoneda(f.importeTotal)}</td>
            `;
            tbodyConfirmar.appendChild(tr);
        }
        if (totalConfirmar) {
            totalConfirmar.textContent = formatearMoneda(suma);
        }
    }

    const detalle = facturas.map((f) => obtenerIdentificadorFactura(f)).join(', ');
    dlgInformar.querySelectorAll<HTMLElement>('[data-campo="cantidad"]').forEach((el) => {
        el.textContent = String(ids.length);
    });
    dlgInformar.querySelectorAll<HTMLElement>('[data-campo="detalle"]').forEach((el) => {
        el.textContent = detalle;
    });

    informeEjecutado = false;
    mostrarFaseArca('confirmar');
    bloquearScrollBody(true);
    dlgInformar.showModal();
}

async function ejecutarInformarArca(): Promise<void> {
    if (!dlgInformar) return;
    const ids = [...seleccionados];
    if (ids.length === 0) return;

    // Apenas se confirma el envío, quitamos la barra inferior de selección
    salirModoSeleccion();

    // Marcamos visualmente en la tabla de fondo las facturas como EN_PROCESO
    for (const id of ids) {
        const trFondo = tbody?.querySelector<HTMLTableRowElement>(`tr[data-id="${id}"]`);
        if (trFondo) {
            trFondo.dataset.estado = 'EN_PROCESO';
            const badgeFondo = trFondo.querySelector<HTMLElement>('[data-campo="estado"]');
            if (badgeFondo) aplicarBadgeEstado(badgeFondo, 'EN_PROCESO');
        }
    }

    // Pasamos a la fase de procesamiento en servidor
    mostrarFaseArca('informando');

    try {
        const resultados = await facturaService.informarArcaLote(ids);
        informeEjecutado = true;

        // Si el usuario mantiene el diálogo abierto, mostramos el desglose detallado
        if (dlgInformar.open) {
            renderResultadoArca(resultados);
            mostrarFaseArca('resultado');
        }

        // Actualizamos la tabla del listado de fondo con los datos persistidos
        await cargarFacturas();

        const autorizadas = resultados.filter((r) => r.estado === 'AUTORIZADA').length;
        const rechazadas = resultados.filter((r) => r.estado === 'RECHAZADA').length;

        if (rechazadas === 0) {
            toast.exito(autorizadas === 1
                ? 'Factura informada correctamente a ARCA.'
                : `Se informaron correctamente ${autorizadas} facturas a ARCA.`);
        } else {
            toast.info(`Envío completado: ${autorizadas} informada(s) y ${rechazadas} rechazada(s).`);
        }
    } catch (err: unknown) {
        const mensajeError = err instanceof Error ? err.message : 'Error al comunicar con el servidor';

        // Restauramos los datos reales del listado
        await cargarFacturas();

        if (dlgInformar.open) {
            mostrarFaseArca('error');
            const errP = dlgInformar.querySelector('#informar-error-mensaje');
            if (errP) errP.textContent = mensajeError;
        } else {
            toast.error(mensajeError);
        }
    }
}

function actualizarFilaResultadoArca(fila: HTMLTableRowElement | undefined, r: ResultadoAutorizacionDTO, _animar: boolean = false): void {
    if (!fila) return;
    const f = facturasPorId.get(r.id);
    const nro = fila.querySelector('[data-campo="factura"]') as HTMLElement;
    if (nro) {
        if (r.numero != null) {
            nro.textContent = `#${r.numero}`;
            nro.className = 'nro-factura';
        } else {
            nro.textContent = f ? obtenerIdentificadorFactura(f) : `#${r.id}`;
        }
    }

    const badge = fila.querySelector('[data-campo="estado"]') as HTMLElement;
    if (badge) {
        aplicarBadgeEstado(badge, r.estado);
        badge.classList.remove('anim-badge-in');
        void badge.offsetWidth;
        badge.classList.add('anim-badge-in');
    }

    const detalleEl = fila.querySelector('[data-campo="detalle"]') as HTMLElement;
    if (detalleEl) {
        const texto = r.mensaje || (r.estado === 'AUTORIZADA' ? 'Factura informada a ARCA' : 'Rechazada');
        detalleEl.innerHTML = `<span class="anim-text-in">${texto}</span>`;
    }

    const enlace = fila.querySelector('[data-campo="enlace"]') as HTMLAnchorElement;
    if (enlace) {
        enlace.style.pointerEvents = '';
        enlace.style.opacity = '1';
    }
}

function renderizarChipsResumen(chips: HTMLElement, resultados: ResultadoAutorizacionDTO[], erroresConexion: number = 0): void {
    const conteo = new Map<EstadoFactura, number>();
    for (const r of resultados) {
        conteo.set(r.estado, (conteo.get(r.estado) ?? 0) + 1);
    }
    chips.replaceChildren();
    for (const [estado, count] of conteo) {
        const span = document.createElement('span');
        aplicarBadgeEstado(span, estado);
        span.textContent = `${span.textContent}: ${count}`;
        chips.appendChild(span);
    }
    if (erroresConexion > 0) {
        const spanErr = document.createElement('span');
        spanErr.className = 'badge badge-rechazada';
        spanErr.textContent = `Sin conexión: ${erroresConexion}`;
        chips.appendChild(spanErr);
    }
}

function renderResultadoArca(resultados: ResultadoAutorizacionDTO[]): void {
    if (!dlgInformar) return;
    const chips = dlgInformar.querySelector('#informar-chips') as HTMLElement;
    const cuerpo = dlgInformar.querySelector('#informar-resultado-body') as HTMLTableSectionElement;
    const tplResultado = document.getElementById('tpl-resultado-fila') as HTMLTemplateElement;

    renderizarChipsResumen(chips, resultados);

    cuerpo.replaceChildren();
    for (const r of resultados) {
        const clone = tplResultado.content.cloneNode(true) as DocumentFragment;
        const fila = clone.querySelector('tr') as HTMLTableRowElement;
        actualizarFilaResultadoArca(fila, r);
        cuerpo.appendChild(fila);
    }
}

// ─── Event Wiring & Inicialización ──────────────────────────────────────────
btnFiltros?.addEventListener('click', () => {
    if (!formFiltros || !btnFiltros) return;
    const colapsado = formFiltros.classList.toggle('is-collapsed');
    btnFiltros.setAttribute('aria-expanded', String(!colapsado));
});

formFiltros?.addEventListener('submit', (e) => {
    e.preventDefault();
    actualizarBadgeFiltros();
    paginaActual = 0;
    void cargarFacturas();
});

document.getElementById('btn-limpiar-filtros')?.addEventListener('click', () => limpiarFiltros());

document.getElementById('btn-abrir-dialogo-cliente')?.addEventListener('click', () => {
    modalCliente.abrir();
});

inputClienteNombre?.addEventListener('click', () => {
    modalCliente.abrir();
});

btnLimpiarCliente?.addEventListener('click', () => {
    if (inputClienteId) inputClienteId.value = '';
    if (inputClienteNombre) inputClienteNombre.value = '';
    if (btnLimpiarCliente) btnLimpiarCliente.hidden = true;
    actualizarBadgeFiltros();
    paginaActual = 0;
    void cargarFacturas();
});

chkSeleccionarTodos?.addEventListener('change', () => {
    if (!tbody) return;
    const marcar = chkSeleccionarTodos.checked;
    tbody.querySelectorAll<HTMLTableRowElement>('tr:not(.no-informable)').forEach((tr) => {
        const id = parseInt(tr.dataset.id || '0', 10);
        const chk = tr.querySelector<HTMLInputElement>('input[name="facturaIds"]');
        if (chk && !chk.disabled) {
            chk.checked = marcar;
            if (marcar) seleccionados.add(id); else seleccionados.delete(id);
            tr.classList.toggle('is-selected', marcar);
        }
    });
    actualizarSeleccion();
});

btnModoInformar?.addEventListener('click', () => {
    if (modoSeleccion) salirModoSeleccion(); else entrarModoSeleccion();
});

btnCancelarSeleccion?.addEventListener('click', () => salirModoSeleccion());
btnInformar?.addEventListener('click', () => abrirInformarArca());

// Menú contextual de fila
menuFactura?.addEventListener('click', (e) => {
    const btn = (e.target as HTMLElement).closest<HTMLButtonElement>('[data-menu]');
    if (!btn || menuFacturaId == null || btn.disabled) return;
    const accion = btn.dataset.menu;
    const id = menuFacturaId;
    cerrarMenuFila();

    if (accion === 'informar') {
        abrirInformarArca(id);
    } else if (accion === 'editar') {
        window.location.href = `factura.html?id=${id}&modo=editar`;
    } else if (accion === 'anular') {
        void anularFactura(id);
    } else if (accion === 'eliminar') {
        void eliminarFactura(id);
    }
});

document.addEventListener('click', (e) => {
    if (!menuFactura?.hidden && !menuFactura?.contains(e.target as Node)) {
        cerrarMenuFila();
    }
    if (!menuExportar?.hidden && !menuExportar?.contains(e.target as Node) && e.target !== btnExportarToggle && !btnExportarToggle?.contains(e.target as Node)) {
        cerrarMenuExportar();
    }
});

// Tecla Escape para accesibilidad
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        if (!menuExportar?.hidden) {
            cerrarMenuExportar();
            return;
        }
        if (!menuFactura?.hidden) {
            cerrarMenuFila();
            return;
        }
        if (modoSeleccion && (!dlgInformar || !dlgInformar.open)) {
            salirModoSeleccion();
        }
    }
});

// Diálogo ARCA envío
dlgInformar?.addEventListener('submit', (e) => {
    const btn = (e.submitter as HTMLButtonElement | null)?.value;
    const form = e.target as HTMLFormElement;
    const fase = form.getAttribute('data-fase');

    if (fase === 'confirmar' && btn === 'confirmar') {
        e.preventDefault();
        void ejecutarInformarArca();
    }
});

const btnCerrarInformando = document.getElementById('btn-cerrar-informando') as HTMLButtonElement | null;
btnCerrarInformando?.addEventListener('click', () => {
    salirModoSeleccion();
    if (dlgInformar?.open) {
        dlgInformar.close();
    }
    toast.info('Informando facturas en segundo plano...');
});

dlgInformar?.addEventListener('close', () => {
    bloquearScrollBody(false);
    salirModoSeleccion();
    if (informeEjecutado) {
        void cargarFacturas();
    }
});

dlgInformar?.addEventListener('cancel', () => {
    bloquearScrollBody(false);
});

// Listener global para asegurar el desbloqueo del scroll en cualquier diálogo
document.querySelectorAll('dialog').forEach((dlg) => {
    dlg.addEventListener('close', () => bloquearScrollBody(false));
    dlg.addEventListener('cancel', () => bloquearScrollBody(false));
});

// ─── Exportación de Reportes (Excel / PDF) ──────────────────────────────────
btnExportarToggle?.addEventListener('click', (e) => {
    e.stopPropagation();
    if (!menuExportar) return;
    const estaOculto = menuExportar.hidden;
    menuExportar.hidden = !estaOculto;
    btnExportarToggle.setAttribute('aria-expanded', String(estaOculto));
});

function cerrarMenuExportar(): void {
    if (!menuExportar || menuExportar.hidden) return;
    menuExportar.hidden = true;
    btnExportarToggle?.setAttribute('aria-expanded', 'false');
}

async function exportarFacturas(tipo: 'excel' | 'pdf', btn?: HTMLButtonElement | null): Promise<void> {
    cerrarMenuExportar();
    const textoOriginal = btn?.innerHTML;
    try {
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = `<span class="spinner" aria-hidden="true"></span> Descargando...`;
        }
        toast.info(`Generando archivo ${tipo.toUpperCase()}...`);
        await facturaService.descargarReporte(tipo);
        toast.exito(`Reporte ${tipo.toUpperCase()} descargado exitosamente.`);
    } catch (error) {
        const msg = error instanceof Error ? error.message : 'Error al exportar.';
        toast.error(`No se pudo descargar el reporte: ${msg}`);
    } finally {
        if (btn && textoOriginal) {
            btn.disabled = false;
            btn.innerHTML = textoOriginal;
        }
    }
}

btnExportarExcel?.addEventListener('click', () => void exportarFacturas('excel', btnExportarExcel));
btnExportarPdf?.addEventListener('click', () => void exportarFacturas('pdf', btnExportarPdf));

// Inicio
actualizarBadgeFiltros();
void cargarFacturas();
