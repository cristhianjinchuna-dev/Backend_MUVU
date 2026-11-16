package com.example.desarrollo;

import com.example.desarrollo.dto.ErrorResponseDTO;
import com.example.desarrollo.exceptions.MuvuException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Todas las excepciones propias (NotFound, Conflict, Forbidden, InvalidOperation, ExternalService...): usan su propio status
    @ExceptionHandler(MuvuException.class)
    public ResponseEntity<ErrorResponseDTO> handleMuvu(MuvuException ex, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), ex.getStatus().value(), ex.getMessage());
        return build(ex.getStatus(), ex.getMessage(), request, null);
    }

    // Errores de @Valid en los DTOs: devuelve el mensaje de cada campo
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Datos inválidos", request, errores);
    }

    // Errores de validación en parámetros de la URL (@Min, @Positive en page, size, radioKm...)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidacionParametros(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(r -> errores.putIfAbsent(
                r.getMethodParameter().getParameterName(),
                r.getResolvableErrors().get(0).getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Parámetros inválidos", request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleBodyIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido o tiene un tipo de dato incorrecto", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTipoIncorrecto(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("El valor '%s' no es válido para el parámetro '%s'", ex.getValue(), ex.getName());
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleParametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("Falta el parámetro obligatorio '%s'", ex.getParameterName());
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Inicio de sesión fallido en {}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos", request, null);
    }

    // Login de una cuenta suspendida por el ADMIN (Usuario.isEnabled() = false)
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponseDTO> handleCuentaSuspendida(DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN,
                "Tu cuenta está suspendida por un administrador. Revisa tu correo para más detalles.", request, null);
    }

    // Petición sin token o con token inválido/expirado (viene desde el filtro de seguridad)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoAutenticado(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión: el token no se envió, es inválido o expiró", request, null);
    }

    // AccessDeniedException de Spring Security (por ejemplo, al fallar un @PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado a {} {}", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = String.format("El método HTTP '%s' no está permitido para esta ruta. Métodos soportados: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "No se puede completar la operación porque el registro está relacionado con otros datos", request, null);
    }

    // Cualquier error no previsto: no se exponen detalles internos al cliente, pero queda en el log
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenerico(Exception ex, HttpServletRequest request) {
        // Excepciones de Spring MVC que ya traen su status (ruta inexistente 404, Content-Type no soportado 415...)
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return build(status, errorResponse.getBody().getDetail(), request, null);
        }
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
    }

    private ResponseEntity<ErrorResponseDTO> build(HttpStatus status, String message,
                                                   HttpServletRequest request, Map<String, String> errores) {
        ErrorResponseDTO body = new ErrorResponseDTO(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(), errores);
        return ResponseEntity.status(status).body(body);
    }
}
