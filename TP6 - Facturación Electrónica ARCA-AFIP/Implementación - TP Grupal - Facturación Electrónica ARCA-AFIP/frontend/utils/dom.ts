/**
 * Utilidades para interactuar con el DOM de forma tipada y segura.
 */

/**
 * Asigna texto a un elemento hijo identificado por un selector dentro de un contenedor.
 */
export function setTexto(contenedor: Element, selector: string, texto: string): void {
    const el = contenedor.querySelector(selector);
    if (el) {
        el.textContent = texto;
    }
}
