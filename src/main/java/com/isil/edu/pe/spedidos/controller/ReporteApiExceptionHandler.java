package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.controller.dto.ErrorResponse;
import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * [PA3] Traduce las excepciones de la API de reportes a respuestas JSON con
 * códigos HTTP coherentes.
 *
 * <p>Se limita a {@link PedidoReporteController} para no alterar el
 * comportamiento de los demás controladores del proyecto base.</p>
 *
 * <ul>
 *   <li>{@link ParametroReporteInvalidoException} (regla del Service) → 400.</li>
 *   <li>{@link MethodArgumentTypeMismatchException} (p. ej. totalMinimo=abc) → 400.</li>
 *   <li>{@link DataAccessException} (base de datos no disponible o error SQL) → 503,
 *       con un mensaje útil y sin exponer el SQL ni la traza al cliente.</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = PedidoReporteController.class)
public class ReporteApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ReporteApiExceptionHandler.class);

  @ExceptionHandler(ParametroReporteInvalidoException.class)
  public ResponseEntity<ErrorResponse> manejarParametroInvalido(
      ParametroReporteInvalidoException e, HttpServletRequest request) {

    return responder(HttpStatus.BAD_REQUEST, e.getMessage(), e.getParametro(), request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> manejarTipoInvalido(
      MethodArgumentTypeMismatchException e, HttpServletRequest request) {

    String mensaje = "El parámetro " + e.getName() + " tiene un formato inválido.";
    return responder(HttpStatus.BAD_REQUEST, mensaje, e.getName(), request);
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> manejarErrorDeDatos(
      DataAccessException e, HttpServletRequest request) {

    // Se registra la causa para diagnóstico y se responde un 503 controlado.
    log.error("Error de acceso a datos en {}", request.getRequestURI(), e);
    return responder(HttpStatus.SERVICE_UNAVAILABLE,
        "No fue posible consultar los reportes en este momento. Intente nuevamente.",
        null, request);
  }

  private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje,
      String parametro, HttpServletRequest request) {

    ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(),
        mensaje, parametro, request.getRequestURI());

    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }
}
