package com.isil.edu.pe.spedidos.exception;

/**
 * [PA3] Señala que un parámetro de un reporte no cumple las reglas de negocio
 * (cliente vacío, totalMinimo negativo o minPedidos menor que 1).
 *
 * <p>La lanza únicamente {@code PedidoReporteServiceImpl}. Los controladores
 * la traducen a una respuesta controlada: HTTP 400 en JSON
 * ({@code ReporteApiExceptionHandler}) o HTTP 400 con mensaje en la JSP
 * ({@code PedidoReporteWebController}). Como la validación ocurre antes de
 * invocar al repositorio, un parámetro inválido nunca llega a ejecutar SQL.</p>
 */
public class ParametroReporteInvalidoException extends RuntimeException {

  private final String parametro;

  public ParametroReporteInvalidoException(String parametro, String message) {
    super(message);
    this.parametro = parametro;
  }

  /**
   * @return nombre del parámetro rechazado, útil para la respuesta de error
   */
  public String getParametro() {
    return parametro;
  }
}
