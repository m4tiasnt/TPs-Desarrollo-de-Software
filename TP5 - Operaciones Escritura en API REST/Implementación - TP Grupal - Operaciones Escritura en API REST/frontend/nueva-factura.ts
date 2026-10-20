import {
    FacturaRequestDTO,
    FacturaDetalleRequestDTO
} from './models/factura.dto.js';

import { FacturaService } from './services/FacturaService.js';

// 1. Instancia del servicio que se comunica con la API de Spring Boot
const facturaService = new FacturaService('http://localhost:8080/api/facturas');

// Estructura extendida para la UI que conserva datos visuales antes de enviar
interface ItemFacturaUI extends FacturaDetalleRequestDTO {
    precioUnitario?: number;
}

// Guardamos temporalmente los renglones de la factura
let itemsFactura: ItemFacturaUI[] = [];

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


// Referencias a los elementos del formulario de ítems
const btnAgregarItem = document.getElementById('btn-agregar-item') as HTMLButtonElement;
const tablaDetallesBody = document.getElementById('tabla-detalles-body') as HTMLTableSectionElement;
const inputArticuloId = document.getElementById('item-articulo-id') as HTMLInputElement;
const inputDescripcion = document.getElementById('item-descripcion') as HTMLInputElement;
const inputCantidad = document.getElementById('item-cantidad') as HTMLInputElement;
const inputPrecio = document.getElementById('item-precio') as HTMLInputElement;
const inputDescuento = document.getElementById('item-descuento') as HTMLInputElement;

// Dibuja las filas en la tabla según lo que haya en itemsFactura (en memoria)
function renderizarTablaDetalles(): void {
    if (!tablaDetallesBody) return;

    if (itemsFactura.length === 0) {
        tablaDetallesBody.innerHTML = `
            <tr id="fila-sin-items">
                <td colspan="7" style="text-align: center; color: var(--color-text-muted); padding: 14px;">
                    No hay renglones cargados. Ingrese los datos arriba y haga clic en "+ Agregar renglón".
                </td>
            </tr>
        `;
        return;
    }

    tablaDetallesBody.innerHTML = '';

    itemsFactura.forEach((item, index) => {
        const fila = document.createElement('tr');

        const bonif = item.porcentajeBonificacion ?? 0;
        const precioTexto = item.precioUnitario ? `$${item.precioUnitario.toFixed(2)}` : 'Seg. lista';
        const subtotalTexto = item.importeSubtotal ? `$${item.importeSubtotal.toFixed(2)}` : 'Automático';
        const artId = item.articuloId ?? item.listaPrecioArticuloId ?? '-';

        fila.innerHTML = `
            <td>${artId}</td>
            <td>${item.descripcion || 'Sin descripción'}</td>
            <td class="num">${item.cantidad}</td>
            <td class="num">${precioTexto}</td>
            <td class="num">${bonif}%</td>
            <td class="num">${subtotalTexto}</td>
            <td class="actions">
                <button type="button" class="btn-icon btn-icon--danger btn-quitar-item" data-index="${index}" aria-label="Quitar renglón" title="Quitar renglón">
                    <svg class="icon" aria-hidden="true"><use href="#i-trash" /></svg>
                </button>
            </td>
        `;

        // Evento para quitar este renglón específico
        const btnQuitar = fila.querySelector('.btn-quitar-item') as HTMLButtonElement;
        btnQuitar.addEventListener('click', () => {
            quitarItem(index);
        });

        tablaDetallesBody.appendChild(fila);
    });
}

// Quita un renglón del arreglo en memoria y actualiza la tabla
function quitarItem(indice: number): void {
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

    // Subtotal estimado para preview en UI
    const precioUnitarioEstimado = precio ?? 0;
    const subtotalEstimado = precioUnitarioEstimado > 0
        ? cantidad * precioUnitarioEstimado * (1 - (descuento / 100))
        : 0;

    // Objeto DTO del renglón con datos UI
    const nuevoDetalle: ItemFacturaUI = {
        articuloId: articuloId,
        listaPrecioArticuloId: articuloId,
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

const formAltaFactura = document.getElementById('form-alta-factura') as HTMLFormElement;
const inputClienteId = document.getElementById('alta-cliente-id') as HTMLInputElement;
const inputPuntoVentaId = document.getElementById('alta-punto-venta-id') as HTMLInputElement;
const inputCondicionIvaId = document.getElementById('alta-condicion-iva-id') as HTMLInputElement;
const inputTipoMonedaId = document.getElementById('alta-tipo-moneda-id') as HTMLInputElement;
const inputObservaciones = document.getElementById('alta-observaciones') as HTMLInputElement;

formAltaFactura?.addEventListener('submit', async (e: Event) => {
    e.preventDefault();
    // Valido que tengo al menos un renglon en memoria
    if (itemsFactura.length === 0) {
        mostrarMensaje('Debe agregar al menos un renglón a la factura antes de guardarla.', 'error');
        return;
    }
    const nuevaFactura: FacturaRequestDTO = {
        clienteId: parseInt(inputClienteId.value),
        puntoVentaId: parseInt(inputPuntoVentaId.value),
        condicionIvaId: parseInt(inputCondicionIvaId.value),
        tipoMonedaId: parseInt(inputTipoMonedaId.value),
        observaciones: inputObservaciones.value.trim() || undefined,
        detalles: itemsFactura.map(item => ({
            articuloId: item.articuloId,
            listaPrecioArticuloId: item.listaPrecioArticuloId,
            descripcion: item.descripcion,
            cantidad: item.cantidad,
            porcentajeBonificacion: item.porcentajeBonificacion
        }))
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
        // Volvemos al listado para ver la factura recién creada
        window.location.href = 'index.html';
    } catch (error: any) {
        console.error('Error al emitir factura:', error);
        mostrarMensaje(`Error al crear la factura: ${error.message || 'Error desconocido'}`, 'error');
    }
});

// Botón Cancelar: vuelve al listado sin guardar
const btnCancelar = document.getElementById('btn-cancelar') as HTMLButtonElement;
btnCancelar?.addEventListener('click', () => {
    window.location.href = 'index.html';
});
