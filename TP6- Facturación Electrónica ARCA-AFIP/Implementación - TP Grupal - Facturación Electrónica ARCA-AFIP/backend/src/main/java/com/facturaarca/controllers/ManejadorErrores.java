package com.facturaarca.controllers;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Traduce las excepciones de la API a un JSON uniforme: { "mensaje": "..." }.
 * El frontend muestra ese mensaje directamente al usuario.
 */
@Slf4j
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejarResponseStatus(ResponseStatusException ex) {
        String mensaje = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("mensaje",
                        "El cuerpo de la petición no es un JSON válido o contiene un valor con formato incorrecto"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("mensaje", "Valor inválido para el parámetro '" + ex.getName() + "'"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("mensaje", "Falta el parámetro '" + ex.getParameterName() + "'"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Los datos enviados no son válidos");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }

    /**
     * Valida restricciones declaradas directamente en parámetros de métodos
     * (por ejemplo, con @Validated y @RequestParam).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, String>> manejarValidacionDeParametros(HandlerMethodValidationException ex) {
        String mensaje = ex.getAllValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream())
                .map(error -> error.getDefaultMessage())
                .filter(texto -> texto != null && !texto.isBlank())
                .findFirst()
                .orElse("Los parámetros de la petición no son válidos");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }

    /**
     * Valida restricciones de Bean Validation fuera de un @RequestBody
     * (por ejemplo, servicios anotados con @Validated).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> manejarViolacionDeRestriccion(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violacion -> violacion.getPropertyPath() + ": " + violacion.getMessage())
                .orElse("Los datos enviados no son válidos");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> manejarTipoDeContenidoNoSoportado(
            HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Map.of("mensaje",
                        "El tipo de contenido de la petición no está soportado. Utilice application/json"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarErrorDeDatos(DataIntegrityViolationException ex) {
        log.warn("Conflicto de integridad de datos en la API: {}", ex.getMostSpecificCause().getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", "No se pudo guardar la información por un conflicto de datos"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> manejarRecursoInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("mensaje", "Recurso no encontrado"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("mensaje", "Método HTTP no soportado para esta ruta"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensaje", "Error interno del servidor"));
    }
}
