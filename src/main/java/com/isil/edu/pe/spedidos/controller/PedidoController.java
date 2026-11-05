package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.PedidoException;
import com.isil.edu.pe.spedidos.service.PedidoService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de presentación para la consulta y el registro de pedidos mediante Spring MVC.
 *
 * <p><strong>Responsabilidad:</strong> atender formularios web bajo la ruta
 * {@code /pedidos}, coordinar la interacción con {@link PedidoService} y devolver
 * nombres lógicos de vistas. Al utilizar {@link Controller @Controller}, este
 * componente renderiza vistas; no es el controlador REST de reportes.</p>
 *
 * <p><strong>Arquitectura y separación de responsabilidades:</strong></p>
 * <pre>
 * Navegador --HTTP--&gt; DispatcherServlet --&gt; PedidoController
 *                                              |
 *                                              v
 *                                         PedidoService
 *                                          /        \
 *                                         v          v
 *                                 PedidoRepository  InventarioClient
 *                                         |          |
 *                                   MySQL / H2   inventario-service
 * </pre>
 * <p>MySQL corresponde a la ejecución normal y H2 al perfil de pruebas.
 * El controlador no conoce el mecanismo de persistencia ni implementa
 * directamente la integración con Inventario: delega ambas tareas al servicio.</p>
 *
 * <p><strong>Contratos web:</strong> {@code GET /pedidos} presenta el formulario
 * y la lista; {@code POST /pedidos} registra un pedido y, si tiene éxito,
 * aplica Post/Redirect/Get (PRG). Los errores de datos o negocio generan
 * {@code 400}; la indisponibilidad del inventario, {@code 503}; y los errores
 * inesperados, {@code 500}. Las solicitudes satisfactorias de listado
 * generan normalmente {@code 200}.</p>
 *
 * @see PedidoService
 * @see Pedido
 * @see Controller
 * @see RequestMapping
 */
@Controller
@RequestMapping("/pedidos")
public class PedidoController {

  private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

  /**
   * Dependencia de la capa de negocio declarada mediante su contrato.
   *
   * <p>La referencia es {@code final}: se asigna en la construcción y no se
   * sustituye durante la vida del controlador. Esto favorece el principio
   * de inversión de dependencias y facilita el uso de dobles de prueba.</p>
   */
  private final PedidoService pedidoService;

  /**
   * Crea el controlador mediante inyección de dependencias por constructor.
   *
   * <p>Spring suministra un bean compatible con {@link PedidoService}; el
   * controlador no instancia la implementación. Como existe un único
   * constructor, no se requiere {@code @Autowired}. Este diseño facilita
   * las pruebas unitarias con Mockito y mantiene desacopladas las capas.</p>
   *
   * @param pedidoService servicio que coordina las operaciones de pedidos
   */
  public PedidoController(PedidoService pedidoService) {
    this.pedidoService = pedidoService;
  }

  /**
   * Presenta la página principal de pedidos para una solicitud HTTP GET.
   *
   * <p><strong>Ruta:</strong> {@code GET /pedidos}. Mediante
   * {@link #cargarDatosVista(Model)}, incorpora al {@link Model} los atributos
   * {@code productos} y {@code pedidos}, utilizados por la vista JSP.</p>
   *
   * <p><strong>Resultado:</strong> retorna {@code pedidos}, un nombre lógico
   * de vista que Spring MVC resuelve con el mecanismo configurado. Si el
   * servicio de Inventario falla, devuelve la vista {@code error} con estado
   * {@code 503}; ante otra excepción de ejecución registra el incidente y
   * devuelve esa vista con estado {@code 500}.</p>
   *
   * <p><strong>Concepto pedagógico:</strong> un método de {@code @Controller}
   * que retorna {@link String} no envía necesariamente ese texto al cliente:
   * en este caso indica qué vista debe renderizarse.</p>
   *
   * @param model contenedor de atributos enviados desde el controlador a la vista
   * @param response respuesta HTTP cuyo estado se ajusta si ocurre un error
   * @return {@code pedidos} cuando la carga es satisfactoria; {@code error} si falla
   * @see GetMapping
   */
  @GetMapping
  public String listar(Model model, HttpServletResponse response) {
    try {
      cargarDatosVista(model);
      return "pedidos";
    } catch (InventarioNoDisponibleException e) {
      return mostrarServicioNoDisponible(model, response, e.getMessage());
    } catch (RuntimeException e) {
      log.error("Error inesperado al cargar la pantalla de pedidos.", e);
      return mostrarErrorGeneral(model, response);
    }
  }

  /**
   * Procesa el formulario de creación de pedidos mediante HTTP POST.
   *
   * <p><strong>Ruta:</strong> {@code POST /pedidos}. Recibe los campos
   * {@code cliente}, {@code productoCodigo} y {@code cantidad} con
   * {@link RequestParam @RequestParam}. Convierte la cantidad de texto a
   * entero y delega el registro en {@link PedidoService#registrarPedido}.</p>
   *
   * <p><strong>Flujo exitoso (PRG):</strong> agrega el identificador del pedido
   * creado a {@link RedirectAttributes} bajo la clave {@code creado} y
   * retorna {@code redirect:/pedidos}. El navegador realiza entonces una
   * solicitud GET, reduciendo los reenvíos accidentales del formulario.</p>
   *
   * <p><strong>Errores:</strong> una cantidad no numérica o una
   * {@link PedidoException} producen {@code 400} y conservan los campos
   * ingresados; una {@link InventarioNoDisponibleException} produce
   * {@code 503}; otros fallos de ejecución se registran y producen
   * {@code 500}. En los casos de error no se aplica la redirección PRG.</p>
   *
   * @param cliente nombre del cliente recibido desde el formulario
   * @param productoCodigo código del producto seleccionado
   * @param cantidad cantidad solicitada como texto, previa a su conversión
   * @param model atributos utilizados para volver a mostrar el formulario o error
   * @param response respuesta HTTP utilizada para establecer el estado de error
   * @param redirectAttributes parámetros transportados a la solicitud GET posterior
   * @return {@code redirect:/pedidos} si se registra; {@code pedidos} o
   *         {@code error} cuando debe presentarse un fallo
   * @see PostMapping
   */
  @PostMapping
  public String registrar(@RequestParam String cliente, @RequestParam String productoCodigo,
      @RequestParam String cantidad, Model model, HttpServletResponse response,
      RedirectAttributes redirectAttributes) {
    try {
      int cantidadNumerica = Integer.parseInt(cantidad);

      Pedido pedido = pedidoService.registrarPedido(cliente, productoCodigo, cantidadNumerica);

      // PRG: POST exitoso -> redirect -> GET.
      redirectAttributes.addAttribute("creado", pedido.getId());
      return "redirect:/pedidos";
    } catch (NumberFormatException e) {
      return mostrarErrorNegocio(model, response, cliente, productoCodigo, cantidad,
          "La cantidad ingresada es inválida.");
    } catch (InventarioNoDisponibleException e) {
      return mostrarServicioNoDisponible(model, response, e.getMessage());
    } catch (PedidoException e) {
      return mostrarErrorNegocio(model, response, cliente, productoCodigo, cantidad,
          e.getMessage());
    } catch (RuntimeException e) {
      log.error("Error inesperado al registrar un pedido.", e);
      return mostrarErrorGeneral(model, response);
    }
  }

  /**
   * Reúne los datos comunes necesarios para renderizar la vista de pedidos.
   *
   * <p>Agrega {@code productos}, obtenidos del servicio de Inventario a través
   * de la capa de negocio, y {@code pedidos}, procedentes de la consulta de
   * pedidos. Centralizar esta preparación evita duplicación entre el listado
   * inicial y la redistribución del formulario después de un error.</p>
   *
   * <p>La consulta se delega en {@link PedidoService}; el controlador no
   * ejecuta consultas SQL ni invoca directamente al cliente HTTP externo.</p>
   *
   * @param model modelo al que se agregan {@code productos} y {@code pedidos}
   * @throws InventarioNoDisponibleException si falla la consulta al inventario
   */
  private void cargarDatosVista(Model model) {
    model.addAttribute("productos", pedidoService.listarProductos());
    model.addAttribute("pedidos", pedidoService.listarPedidos());
  }

  /**
   * Reconstruye el formulario cuando los datos son inválidos o se incumple
   * una regla de negocio.
   *
   * <p>Establece {@code 400 Bad Request} y conserva el contexto ingresado en
   * {@code clienteIngresado}, {@code productoCodigoIngresado} y
   * {@code cantidadIngresada}; el mensaje se expone bajo {@code error}.
   * Luego vuelve a cargar productos y pedidos para presentar {@code pedidos}.</p>
   *
   * <p>Si falla Inventario durante la recarga, prevalece la indisponibilidad
   * de la dependencia: se devuelve {@code error} con {@code 503}.</p>
   *
   * @param model modelo que recibe valores previos y mensaje de validación
   * @param response respuesta cuyo estado se establece en {@code 400} o {@code 503}
   * @param cliente nombre del cliente ingresado
   * @param productoCodigo código del producto ingresado
   * @param cantidad cantidad ingresada originalmente como texto
   * @param mensaje explicación del error que se mostrará al usuario
   * @return {@code pedidos} o, si falla Inventario, {@code error}
   */
  private String mostrarErrorNegocio(Model model, HttpServletResponse response, String cliente,
      String productoCodigo, String cantidad, String mensaje) {
    response.setStatus(HttpStatus.BAD_REQUEST.value());

    model.addAttribute("clienteIngresado", cliente);
    model.addAttribute("productoCodigoIngresado", productoCodigo);
    model.addAttribute("cantidadIngresada", cantidad);
    model.addAttribute("error", mensaje);

    try {
      cargarDatosVista(model);
      return "pedidos";
    } catch (InventarioNoDisponibleException e) {
      return mostrarServicioNoDisponible(model, response, e.getMessage());
    }
  }

  /**
   * Presenta un error de dependencia externa cuando Inventario no responde.
   *
   * <p>Establece {@code 503 Service Unavailable}, incorpora el texto recibido
   * en el atributo {@code error} y selecciona la vista lógica {@code error}.
   * Es un ejemplo de traducción de una excepción de integración a un estado
   * HTTP comprensible para el consumidor de la aplicación.</p>
   *
   * @param model modelo que transporta el mensaje de indisponibilidad
   * @param response respuesta HTTP cuyo estado se establece en {@code 503}
   * @param mensaje descripción del fallo de disponibilidad
   * @return nombre lógico de la vista {@code error}
   */
  private String mostrarServicioNoDisponible(
      Model model,
      HttpServletResponse response,
      String mensaje
  ) {
    response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    model.addAttribute("error", mensaje);
    return "error";
  }

  /**
   * Presenta una respuesta genérica ante errores de ejecución inesperados.
   *
   * <p>Establece {@code 500 Internal Server Error} y utiliza un mensaje
   * neutral en la vista {@code error}. La excepción original se registra
   * en los puntos de captura, pero sus detalles técnicos no se exponen en
   * la interfaz, reduciendo la divulgación accidental de información interna.</p>
   *
   * @param model modelo que transporta el mensaje seguro para el usuario
   * @param response respuesta HTTP cuyo estado se establece en {@code 500}
   * @return nombre lógico de la vista {@code error}
   */
  private String mostrarErrorGeneral(Model model, HttpServletResponse response) {
    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    model.addAttribute("error", "Ocurrió un error interno al procesar la solicitud.");
    return "error";
  }

}
