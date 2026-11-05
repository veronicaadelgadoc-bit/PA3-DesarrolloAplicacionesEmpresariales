package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * [PA3] Controlador Spring MVC de la vista de reportes.
 *
 * <p>GET /reportes/pedidos carga en el {@link Model} el resumen existente, la
 * búsqueda avanzada (si se envió) y los clientes frecuentes, y devuelve el
 * nombre lógico {@code "reportes-pedidos"}. El ViewResolver configurado en
 * {@code application.properties} (prefijo {@code /WEB-INF/views/}, sufijo
 * {@code .jsp}) lo resuelve a {@code /WEB-INF/views/reportes-pedidos.jsp}.
 * El controlador no construye HTML.</p>
 *
 * <p>Reutiliza {@link PedidoReporteService}, el mismo servicio que usa la API
 * JSON ({@link PedidoReporteController}); por eso no repite SQL ni reglas de
 * validación: solo traduce {@link ParametroReporteInvalidoException} a un
 * mensaje en la vista con estado HTTP 400.</p>
 *
 * <p>El acceso está restringido en {@code SecurityConfig} a los roles
 * OPERADOR y CONSULTA; un usuario anónimo es redirigido al login.</p>
 */
@Controller
@RequestMapping("/reportes")
public class PedidoReporteWebController {

  private static final Logger log = LoggerFactory.getLogger(PedidoReporteWebController.class);

  /** Valor inicial del formulario de clientes frecuentes (dato de presentación). */
  static final int MIN_PEDIDOS_POR_DEFECTO = 2;

  static final String VISTA = "reportes-pedidos";

  private final PedidoReporteService reporteService;

  public PedidoReporteWebController(PedidoReporteService reporteService) {
    this.reporteService = reporteService;
  }

  /**
   * GET /reportes/pedidos[?cliente=..&amp;totalMinimo=..][&amp;minPedidos=..]
   */
  @GetMapping("/pedidos")
  public String verReportes(
      @RequestParam(required = false) String cliente,
      @RequestParam(required = false) BigDecimal totalMinimo,
      @RequestParam(required = false) Integer minPedidos,
      Model model,
      HttpServletResponse response) {

    // 1. Resumen existente (JdbcTemplate.queryForObject).
    model.addAttribute("resumen", reporteService.obtenerResumen());

    // 2. Búsqueda avanzada: solo si el usuario envió el formulario.
    model.addAttribute("clienteIngresado", cliente);
    model.addAttribute("totalMinimoIngresado", totalMinimo);

    boolean busquedaSolicitada = cliente != null || totalMinimo != null;
    model.addAttribute("busquedaSolicitada", busquedaSolicitada);

    if (busquedaSolicitada) {
      try {
        model.addAttribute("pedidosEncontrados",
            reporteService.buscarPedidosPorClienteYTotalMinimo(cliente, totalMinimo));
      } catch (ParametroReporteInvalidoException e) {
        model.addAttribute("errorBusqueda", e.getMessage());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
      }
    }

    // 3. Clientes frecuentes (umbral inicial 2; la validación la hace el Service).
    int umbral = minPedidos != null ? minPedidos : MIN_PEDIDOS_POR_DEFECTO;
    model.addAttribute("minPedidosIngresado", umbral);

    try {
      model.addAttribute("clientesFrecuentes", reporteService.listarClientesFrecuentes(umbral));
    } catch (ParametroReporteInvalidoException e) {
      model.addAttribute("errorClientes", e.getMessage());
      response.setStatus(HttpStatus.BAD_REQUEST.value());
    }

    return VISTA;
  }

  /**
   * Parámetro con formato inválido (p. ej. totalMinimo=abc o minPedidos=dos):
   * se responde 400 y la misma vista con un mensaje, en lugar de la página de
   * error por defecto del contenedor.
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public String manejarFormatoInvalido(MethodArgumentTypeMismatchException e,
      Model model, HttpServletResponse response) {

    response.setStatus(HttpStatus.BAD_REQUEST.value());
    model.addAttribute("resumen", reporteService.obtenerResumen());
    model.addAttribute("minPedidosIngresado", MIN_PEDIDOS_POR_DEFECTO);
    model.addAttribute("errorGeneral",
        "El parámetro " + e.getName() + " tiene un formato inválido.");
    return VISTA;
  }

  /**
   * Error de acceso a datos (p. ej. MySQL detenido): se registra la causa y se
   * muestra la página de error con HTTP 503, sin exponer detalles técnicos.
   */
  @ExceptionHandler(DataAccessException.class)
  public String manejarErrorDeDatos(DataAccessException e, Model model,
      HttpServletResponse response) {

    log.error("Error de acceso a datos al cargar la vista de reportes.", e);
    response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    model.addAttribute("error",
        "No fue posible consultar los reportes en este momento. Intente nuevamente.");
    return "error";
  }
}
