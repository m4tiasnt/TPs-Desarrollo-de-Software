import { FacturaService } from './services/FacturaService.js';
// 1. Instancia del servicio que se comunica con la API de Spring Boot
const facturaService = new FacturaService('http://localhost:8080/api/facturas');
// Guardamos temporalmente los renglones de la factura 
let itemsFactura = [];
// Función para mostrar avisos en el sistema
function mostrarMensaje(texto, tipo) {
    const contenedor = document.getElementById('mensaje-feedback');
    if (!contenedor)
        return;
    contenedor.textContent = texto;
    contenedor.className = tipo;
    contenedor.style.display = 'block';
    setTimeout(() => {
        contenedor.style.display = 'none';
    }, 5000);
}
// Referencias a los elementos del formulario de ítems
const btnAgregarItem = document.getElementById('btn-agregar-item');
const tablaDetallesBody = document.getElementById('tabla-detalles-body');
const inputArticuloId = document.getElementById('item-articulo-id');
const inputDescripcion = document.getElementById('item-descripcion');
const inputCantidad = document.getElementById('item-cantidad');
const inputPrecio = document.getElementById('item-precio');
const inputDescuento = document.getElementById('item-descuento');
// Dibuja las filas en la tabla según lo que haya en itemsFactura (en memoria)
function renderizarTablaDetalles() {
    if (!tablaDetallesBody)
        return;
    if (itemsFactura.length === 0) {
        tablaDetallesBody.innerHTML = `
            <tr id="fila-sin-items">
                <td colspan="7" style="text-align: center; color: #94a3b8; padding: 14px;">
                    No hay renglones cargados. Ingrese los datos arriba y haga clic en "+ Agregar Renglón".
                </td>
            </tr>
        `;
        return;
    }
    tablaDetallesBody.innerHTML = '';
    itemsFactura.forEach((item, index) => {
        const fila = document.createElement('tr');
        const precio = item.precioUnitario ?? 0;
        const bonif = item.porcentajeBonificacion ?? 0;
        const subtotal = item.importeSubtotal ?? (item.cantidad * precio * (1 - (bonif / 100)));
        fila.innerHTML = `
            <td>${item.articuloId}</td>
            <td>${item.descripcion || 'Sin descripción'}</td>
            <td>${item.cantidad}</td>
            <td>$${precio.toFixed(2)}</td>
            <td>${bonif}%</td>
            <td>$${subtotal.toFixed(2)}</td>
            <td>
                <button type="button" class="btn-danger btn-quitar-item" data-index="${index}">
                    Quitar
                </button>
            </td>
        `;
        // Evento para quitar este renglón específico
        const btnQuitar = fila.querySelector('.btn-quitar-item');
        btnQuitar.addEventListener('click', () => {
            quitarItem(index);
        });
        tablaDetallesBody.appendChild(fila);
    });
}
// Quita un renglón del arreglo en memoria y actualiza la tabla
function quitarItem(indice) {
    itemsFactura.splice(indice, 1);
    renderizarTablaDetalles();
}
// evento cuando tocan el boton de agregar renglon a la factura
btnAgregarItem?.addEventListener('click', () => {
    const articuloId = parseInt(inputArticuloId.value);
    const cantidad = parseFloat(inputCantidad.value);
    const descripcion = inputDescripcion.value.trim();
    const precio = inputPrecio.value ? parseFloat(inputPrecio.value) : undefined;
    const descuento = inputDescuento.value ? parseFloat(inputDescuento.value) : 0;
    // Validaciones
    if (isNaN(articuloId) || articuloId <= 0) {
        mostrarMensaje('Debe ingresar un Artículo ID válido mayor a 0.', 'error');
        inputArticuloId.focus();
        return;
    }
    if (isNaN(cantidad) || cantidad <= 0) {
        mostrarMensaje('Debe ingresar una cantidad válida mayor a 0.', 'error');
        inputCantidad.focus();
        return;
    }
    // Subtotal estimado
    const precioUnitarioEstimado = precio ?? 0;
    const subtotalEstimado = cantidad * precioUnitarioEstimado * (1 - (descuento / 100));
    // Objeto DTO del renglón
    const nuevoDetalle = {
        articuloId: articuloId,
        descripcion: descripcion || undefined,
        cantidad: cantidad,
        precioUnitario: precio,
        porcentajeBonificacion: descuento,
        importeSubtotal: subtotalEstimado
    };
    // Guardar, refrescar tabla, y limpio imputs 
    itemsFactura.push(nuevoDetalle);
    renderizarTablaDetalles();
    inputArticuloId.value = '';
    inputDescripcion.value = '';
    inputCantidad.value = '';
    inputPrecio.value = '';
    inputDescuento.value = '';
    mostrarMensaje('Renglón agregado a la factura correctamente.', 'exito');
});
const formAltaFactura = document.getElementById('form-alta-factura');
const inputClienteId = document.getElementById('alta-cliente-id');
const inputPuntoVentaId = document.getElementById('alta-punto-venta-id');
const inputCondicionIvaId = document.getElementById('alta-condicion-iva-id');
const inputTipoMonedaId = document.getElementById('alta-tipo-moneda-id');
const inputObservaciones = document.getElementById('alta-observaciones');
formAltaFactura?.addEventListener('submit', async (e) => {
    e.preventDefault();
    //Valido que tengo al menos un renglon en memoria
    if (itemsFactura.length === 0) {
        mostrarMensaje('Debe agregar al menos un renglón a la factura antes de guardarla.', 'error');
        return;
    }
    const nuevaFactura = {
        clienteId: parseInt(inputClienteId.value),
        puntoVentaId: parseInt(inputPuntoVentaId.value),
        condicionIvaId: parseInt(inputCondicionIvaId.value),
        tipoMonedaId: parseInt(inputTipoMonedaId.value),
        observaciones: inputObservaciones.value.trim() || undefined,
        detalles: [...itemsFactura]
    };
    try {
        mostrarMensaje('Enviando factura al servidor...', 'exito');
        // Llamo para crear
        const facturaCreada = await facturaService.crearFactura(nuevaFactura);
        mostrarMensaje(`¡Factura N° ${facturaCreada.numeroFactura} creada con éxito!`, 'exito');
        // Limpio formulario y vacio el estado en memoria
        formAltaFactura.reset();
        itemsFactura = [];
        renderizarTablaDetalles();
        // Refresco el listado general de comprobantes
        if (typeof cargarFacturas === 'function') {
            await cargarFacturas();
        }
    }
    catch (error) {
        console.error('Error al emitir factura:', error);
        mostrarMensaje(`Error al crear la factura: ${error.message || 'Error desconocido'}`, 'error');
    }
});
const formFiltros = document.getElementById('form-filtros');
const btnLimpiarFiltros = document.getElementById('btn-limpiar-filtros');
const inputFiltroFechaDesde = document.getElementById('filtro-fecha-desde');
const inputFiltroFechaHasta = document.getElementById('filtro-fecha-hasta');
const selectFiltroEstado = document.getElementById('filtro-estado');
const inputFiltroMontoMinimo = document.getElementById('filtro-monto-minimo');
// Evento al hacer clic en buscar comprobantes 
formFiltros?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const filtros = {
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
const tablaFacturasBody = document.getElementById('tabla-facturas-body');
// Función para consultar facturas a la API y mandarlas a dibujar
async function cargarFacturas(filtros) {
    if (!tablaFacturasBody)
        return;
    try {
        tablaFacturasBody.innerHTML = `
            <tr id="fila-cargando">
                <td colspan="9" style="text-align: center; color: #64748b; padding: 16px;">
                    Cargando facturas...
                </td>
            </tr>
        `;
        // Llamamos al método GET del servicio
        const facturas = await facturaService.obtenerFacturas(filtros);
        renderizarTablaFacturas(facturas);
    }
    catch (error) {
        console.error('Error al cargar facturas:', error);
        tablaFacturasBody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; color: #dc2626; padding: 16px;">
                    Error al cargar comprobantes: ${error.message || 'Error de conexión con la API'}
                </td>
            </tr>
        `;
        mostrarMensaje('No se pudieron obtener las facturas del servidor.', 'error');
    }
}
function renderizarTablaFacturas(facturas) {
    if (!tablaFacturasBody)
        return;
    if (!facturas || facturas.length === 0) {
        tablaFacturasBody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; color: #64748b; padding: 16px;">
                    No se encontraron comprobantes registrados.
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
        const colorEstado = estado === 'ANULADA' ? '#dc2626' : '#16a34a';
        fila.innerHTML = `
            <td><strong>#${factura.numeroFactura}</strong></td>
            <td>${fechaStr}</td>
            <td>${factura.clienteDenominacion}</td>
            <td>${factura.condicionIva}</td>
            <td>${factura.puntoVentaDescripcion}</td>
            <td><strong>$${factura.importeTotal.toFixed(2)}</strong></td>
            <td>${factura.cantidadItems}</td>
            <td style="color: ${colorEstado}; font-weight: bold;">${estado}</td>
            <td style="text-align: center; white-space: nowrap;">
                <button type="button" class="btn-anular btn-secondary" style="margin-right: 5px; font-size: 0.8rem; padding: 4px 8px;" ${estado === 'ANULADA' ? 'disabled' : ''}>
                    🚫 Anular
                </button>
                <button type="button" class="btn-eliminar btn-danger" style="font-size: 0.8rem; padding: 4px 8px;">
                    🗑️ Eliminar
                </button>
            </td>
        `;
        // Evento para ANULAR la factura 
        const btnAnular = fila.querySelector('.btn-anular');
        if (btnAnular && estado !== 'ANULADA') {
            btnAnular.addEventListener('click', async () => {
                const idFactura = factura.id ?? factura.numeroFactura;
                const confirmar = confirm(`¿Desea anular la factura N° ${factura.numeroFactura}?`);
                if (!confirmar)
                    return;
                try {
                    mostrarMensaje(`Anulando factura N° ${factura.numeroFactura}...`, 'exito');
                    await facturaService.actualizarEstado(idFactura, 'ANULADA', 'Anulada desde la UI web');
                    mostrarMensaje(`Factura N° ${factura.numeroFactura} anulada con éxito.`, 'exito');
                    await cargarFacturas(); // Refrescamos la tabla
                }
                catch (error) {
                    console.error('Error al anular:', error);
                    mostrarMensaje(`Error al anular factura: ${error.message}`, 'error');
                }
            });
        }
        // Evento para ELIMINAR la factura (DELETE /api/facturas/{id})
        const btnEliminar = fila.querySelector('.btn-eliminar');
        btnEliminar?.addEventListener('click', async () => {
            const idFactura = factura.id ?? factura.numeroFactura;
            const confirmar = confirm(`¿Está seguro de ELIMINAR físicamente la factura N° ${factura.numeroFactura}? Esta acción no se puede deshacer.`);
            if (!confirmar)
                return;
            try {
                mostrarMensaje(`Eliminando factura N° ${factura.numeroFactura}...`, 'exito');
                await facturaService.eliminarFactura(idFactura);
                mostrarMensaje(`Factura N° ${factura.numeroFactura} eliminada correctamente.`, 'exito');
                await cargarFacturas(); // Refrescamos la tabla
            }
            catch (error) {
                console.error('Error al eliminar:', error);
                mostrarMensaje(`Error al eliminar factura: ${error.message}`, 'error');
            }
        });
        tablaFacturasBody.appendChild(fila);
    });
}
window.addEventListener('DOMContentLoaded', async () => {
    await cargarFacturas();
});
