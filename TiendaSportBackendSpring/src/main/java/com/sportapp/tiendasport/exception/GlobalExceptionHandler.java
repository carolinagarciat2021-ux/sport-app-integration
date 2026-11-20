package com.sportapp.tiendasport.exception;

import com.sportapp.tiendasport.dto.MensajeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.NoSuchElementException;

/**
 * Convierte cualquier excepción en una respuesta JSON con el mismo formato
 * de toda la API: {"error": "mensaje"}. Los errores internos se registran en
 * el log, pero NO se muestran detalles técnicos al usuario.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MensajeResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(MensajeResponse.error(mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MensajeResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(MensajeResponse.error("El cuerpo de la petición no es válido"));
    }

    // Datos de entrada o reglas de negocio no cumplidas (monto mínimo, estado inválido...)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MensajeResponse> manejarArgumento(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(MensajeResponse.error(ex.getMessage()));
    }

    // Conflictos con el estado actual (sin stock, pedido ya anulado, pedido ya confirmado...)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<MensajeResponse> manejarEstado(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(MensajeResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<MensajeResponse> manejarNoEncontrado(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(MensajeResponse.error(ex.getMessage()));
    }

    // Correo o identificación repetidos (índices UNIQUE) y otras violaciones de la base de datos
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MensajeResponse> manejarIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest()
                .body(MensajeResponse.error("Ya existe un registro con esos datos (por ejemplo, un correo, una identificación o un monto repetido)"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<MensajeResponse> manejarArchivoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(MensajeResponse.error("La imagen es demasiado grande (máximo 3 MB)."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MensajeResponse> manejarAccesoDenegado(AccessDeniedException ex) {
        String mensaje = ex.getMessage() != null && !ex.getMessage().equalsIgnoreCase("Access Denied")
                ? ex.getMessage()
                : "No tienes permisos suficientes para realizar esta acción";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(MensajeResponse.error(mensaje));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MensajeResponse> manejarCredenciales(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MensajeResponse.error("Credenciales inválidas"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MensajeResponse> manejarGenerico(Exception ex) {
        // Errores de Spring MVC (405, 404, 415...) conservan su código en vez de volverse 500
        if (ex instanceof ErrorResponse er) {
            HttpStatus estado = HttpStatus.resolve(er.getStatusCode().value());
            return ResponseEntity.status(er.getStatusCode())
                    .body(MensajeResponse.error(estado != null ? estado.getReasonPhrase() : "Error en la petición"));
        }
        log.error("Error interno no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MensajeResponse.error("Error interno del servidor"));
    }
}
