package com.isil.edu.pe.spedidos.controller.dto;

import java.time.Instant;

/**
 * [PA3] Cuerpo JSON uniforme para los errores de los endpoints de reportes.
 *
 * @param timestamp momento del error (ISO-8601, UTC)
 * @param status    código HTTP
 * @param error     descripción corta del código HTTP
 * @param mensaje   mensaje comprensible para el consumidor de la API
 * @param parametro parámetro rechazado, o {@code null} si no aplica
 * @param path      ruta solicitada
 */
public record ErrorResponse(
    String timestamp,
    int status,
    String error,
    String mensaje,
    String parametro,
    String path
) {

  public static ErrorResponse of(int status, String error, String mensaje,
      String parametro, String path) {
    return new ErrorResponse(Instant.now().toString(), status, error, mensaje, parametro, path);
  }
}
