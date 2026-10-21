import {
    FacturaReporteDTO,
    FacturaFiltrosDTO
} from './models/factura.dto.js';

import { FacturaService } from './services/FacturaService.js';

// 1. Instancia del servicio que se comunica con la API de Spring Boot
const facturaService = new FacturaService('http://localhost:8080/api/facturas');

// Función para mostrar avisos en el sistema
function mostrarMensaje(texto: string, tipo: 'exito' | 'error'): void {
    const contenedor = document.getElementById('mensaje-feedback');
    if (!contenedor) return;

    contenedor.textContent = texto;
    contenedor.className = tipo;
    contenedor.style.display = 'block';
    setTimeout(() => {
        contenedor.style.display = 'none';
    }, 5000);
}

// Confirmación usando el <dialog> del diseño (reemplaza al confirm() nativo).
// Devuelve true si el usuario confirma. Si el diálogo no existe, cae al confirm().
function confirmarDialogo(dialogId: string, numeroFactura: number | string): Promise<boolean> {
    const dialogo = document.getElementById(dialogId) as HTMLDialogElement | null;
    if (!dialogo) {
        return Promise.resolve(confirm(`¿Confirmar acción sobre la factura N° ${numeroFactura}?`));
    }

    dialogo.querySelectorAll('[data-campo="numeroFactura"]').forEach((el) => {
        el.textContent = String(numeroFactura);
    });

    return new Promise((resolve) => {
        const alCerrar = () => {
            dialogo.removeEventListener('close', alCerrar);
            resolve(dialogo.returnValue === 'confirmar');
        };
        dialogo.addEventListener('close', alCerrar);
        dialogo.returnValue = 'cancelar';
        dialogo.showModal();
    });
}


// --- Formulario de Búsqueda (7.a) ---
const formFiltros = document.getElementById('form-filtros') as HTMLFormElement;
const btnLimpiarFiltros = document.getElementById('btn-limpiar-filtros') as HTMLButtonElement;
const inputFiltroFechaDesde = document.getElementById('filtro-fecha-desde') as HTMLInputElement;
const inputFiltroFechaHasta = document.getElementById('filtro-fecha-hasta') as HTMLInputElement;
const selectFiltroEstado = document.getElementById('filtro-estado') as HTMLSelectElement;
const inputFiltroMontoMinimo = document.getElementById('filtro-monto-minimo') as HTMLInputElement;

// En celular los filtros arrancan plegados: este botón alterna la clase is-collapsed
const btnFiltros = document.getElementById('btn-filtros') as HTMLButtonElement;
btnFiltros?.addEventListener('click', () => {
    const plegado = formFiltros?.classList.toggle('is-collapsed');
    btnFiltros.setAttribute('aria-expanded', String(!plegado));
});

// Evento al hacer clic en buscar comprobantes
formFiltros?.addEventListener('submit', async (e: Event) => {
    e.preventDefault();

    const filtros: FacturaFiltrosDTO = {
        fechaDesde: inputFiltroFechaDesde.value || undefined,
        fechaHasta: inputFiltroFechaHasta.value || undefined,
        estado: selectFiltroEstado.value || undefined,
        montoMinimo: inputFiltroMontoMinimo.value ? parseFloat(inputFiltroMontoMinimo.value) : undefined
    };
    await cargarFacturas(filtros);
});

// Evento al hacer clic en limpiar filtros
btnLimpiarFiltros?.addEventListener('click', async () => {
    formFiltros.reset();
    await cargarFacturas();
});


// LISTADO DE FACTURAS Y ACCIONES POR FILA (ANULAR / ELIMINAR)

const tablaFacturasBody = document.getElementById('tabla-facturas-body') as HTMLTableSectionElement;

// Función para consultar facturas a la API y mandarlas a dibujar
async function cargarFacturas(filtros?: FacturaFiltrosDTO): Promise<void> {
    if (!tablaFacturasBody) return;

    try {
        tablaFacturasBody.innerHTML = `
            <tr id="fila-cargando">
                <td colspan="9" style="padding: 0;">
                    <div class="skeleton-row">
                        <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
                    </div>
                    <div class="skeleton-row">
                        <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
                    </div>
                    <div class="skeleton-row">
                        <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
                    </div>
                </td>
            </tr>
        `;

        // Llamamos al método GET del servicio
        const facturas = await facturaService.obtenerFacturas(filtros);
        renderizarTablaFacturas(facturas);

    } catch (error: any) {
        console.error('Error al cargar facturas:', error);
        tablaFacturasBody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; color: var(--color-danger-text); padding: 16px;">
                    Error al cargar comprobantes: ${error.message || 'Error de conexión con la API'}
                </td>
            </tr>
        `;
        mostrarMensaje('No se pudieron obtener las facturas del servidor.', 'error');
    }
}

function renderizarTablaFacturas(facturas: FacturaReporteDTO[]): void {
    if (!tablaFacturasBody) return;

    if (!facturas || facturas.length === 0) {
        tablaFacturasBody.innerHTML = `
            <tr>
                <td colspan="9" style="padding: 0;">
                    <div class="empty-state">
                        <svg class="icon" aria-hidden="true"><use href="#i-file"/></svg>
                        <strong>No se encontraron comprobantes</strong>
                        <span class="muted">Probá cambiando los filtros de búsqueda.</span>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tablaFacturasBody.innerHTML = '';

    facturas.forEach((factura) => {
        const fila = document.createElement('tr');

        const fechaStr = factura.fechaEmision ? String(factura.fechaEmision).substring(0, 10) : '-';
        const estado = factura.estado || 'EMITIDA';
        const claseBadge = estado === 'ANULADA'
            ? 'badge-anulada'
            : (estado === 'PENDIENTE' ? 'badge-pendiente' : 'badge-emitida');

        fila.innerHTML = `
            <td data-label="N° Factura"><strong>#${factura.numeroFactura}</strong></td>
            <td data-label="Fecha emisión">${fechaStr}</td>
            <td data-label="Cliente">${factura.clienteDenominacion}</td>
            <td data-label="Cond. IVA">${factura.condicionIva}</td>
            <td data-label="Pto. venta">${factura.puntoVentaDescripcion}</td>
            <td data-label="Total ($)" class="num"><strong>$${factura.importeTotal.toFixed(2)}</strong></td>
            <td data-label="Cant. ítems" class="num">${factura.cantidadItems}</td>
            <td data-label="Estado"><span class="badge ${claseBadge}">${estado}</span></td>
            <td class="actions">
                <button type="button" class="btn btn-secondary btn--sm btn-anular" ${estado === 'ANULADA' ? 'disabled' : ''}>
                    <svg class="icon" aria-hidden="true"><use href="#i-ban" /></svg>
                    Anular
                </button>
                <button type="button" class="btn btn-danger btn--sm btn-eliminar">
                    <svg class="icon" aria-hidden="true"><use href="#i-trash" /></svg>
                    Eliminar
                </button>
            </td>
        `;

        // Evento para ANULAR la factura
        const btnAnular = fila.querySelector('.btn-anular') as HTMLButtonElement;
        if (btnAnular && estado !== 'ANULADA') {
            btnAnular.addEventListener('click', async () => {
                const idFactura = factura.id;
                const confirmar = await confirmarDialogo('dlg-anular', factura.numeroFactura);
                if (!confirmar) return;

                try {
                    mostrarMensaje(`Anulando factura N° ${factura.numeroFactura}...`, 'exito');
                    // Solo se envía el estado: las observaciones originales no se pisan.
                    await facturaService.actualizarEstado(idFactura, 'ANULADA');
                    mostrarMensaje(`Factura N° ${factura.numeroFactura} anulada con éxito.`, 'exito');
                    await cargarFacturas(); // Refrescamos la tabla
                } catch (error: any) {
                    console.error('Error al anular:', error);
                    mostrarMensaje(`Error al anular factura: ${error.message}`, 'error');
                }
            });
        }

        // Evento para DAR DE BAJA la factura (DELETE /api/facturas/{id})
        const btnEliminar = fila.querySelector('.btn-eliminar') as HTMLButtonElement;
        btnEliminar?.addEventListener('click', async () => {
            const idFactura = factura.id;
            const confirmar = await confirmarDialogo('dlg-eliminar', factura.numeroFactura);
            if (!confirmar) return;

            try {
                mostrarMensaje(`Dando de baja la factura N° ${factura.numeroFactura}...`, 'exito');
                await facturaService.eliminarFactura(idFactura);
                mostrarMensaje(`Factura N° ${factura.numeroFactura} dada de baja correctamente.`, 'exito');
                await cargarFacturas(); // Refrescamos la tabla
            } catch (error: any) {
                console.error('Error al dar de baja:', error);
                mostrarMensaje(`Error al dar de baja la factura: ${error.message}`, 'error');
            }
        });

        tablaFacturasBody.appendChild(fila);
    });
}

window.addEventListener('DOMContentLoaded', async () => {
    await cargarFacturas();
});
