/// <reference types="vite/client" />

/**
 * URL base de la API REST del backend.
 * Se puede sobreescribir con la variable de entorno VITE_API_URL.
 */
export const API_BASE_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api').replace(/\/+$/, '');
