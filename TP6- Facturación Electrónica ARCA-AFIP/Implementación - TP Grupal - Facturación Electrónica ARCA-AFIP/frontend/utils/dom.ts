/**
 * Utilidades para interactuar con el DOM de forma tipada y segura.
 */

/**
 * Obtiene un elemento del DOM por su ID, lanzando un error claro si no existe.
 */
export function getElement<T extends HTMLElement>(id: string): T {
    const el = document.getElementById(id) as T | null;
    if (!el) {
        throw new Error(`Elemento con id "${id}" no encontrado en el DOM.`);
    }
    return el;
}

/**
 * Clona el contenido de un elemento <template> dado su ID.
 */
export function cloneTemplate(templateId: string): DocumentFragment {
    const tpl = getElement<HTMLTemplateElement>(templateId);
    return tpl.content.cloneNode(true) as DocumentFragment;
}

/**
 * Asigna texto a un elemento hijo identificado por un selector dentro de un contenedor.
 */
export function setTexto(contenedor: Element, selector: string, texto: string): void {
    const el = contenedor.querySelector(selector);
    if (el) {
        el.textContent = texto;
    }
}

/**
 * Muestra u oculta un elemento alternando el atributo `hidden`.
 */
export function toggleHidden(elemento: HTMLElement | null, hidden: boolean): void {
    if (elemento) {
        elemento.hidden = hidden;
    }
}

/**
 * Función debounce para demorar la ejecución de búsquedas mientras el usuario escribe.
 */
export function debounce<T extends (...args: any[]) => void>(fn: T, delayMs: number = 250): (...args: Parameters<T>) => void {
    let timer: number | undefined;
    return (...args: Parameters<T>) => {
        if (timer !== undefined) {
            window.clearTimeout(timer);
        }
        timer = window.setTimeout(() => {
            fn(...args);
        }, delayMs);
    };
}
