package com.isil.edu.pe.spedidos.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import com.isil.edu.pe.spedidos.controller.dto.ClienteFrecuenteDto;
import com.isil.edu.pe.spedidos.controller.dto.PedidoResponse;
import com.isil.edu.pe.spedidos.controller.dto.ProductoVendidoDto;
import com.isil.edu.pe.spedidos.controller.dto.ResumenPedidosDto;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para reportes de pedidos.
 * Proporciona endpoints para listar pedidos por cliente, obtener resumen de pedidos
 * y listar productos más vendidos con enlaces HAL.
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoReporteController {

  private final PedidoReporteService
      reporteService;

  public PedidoReporteController(PedidoReporteService reporteService) {
    this.reporteService =
        reporteService;
  }

  /**
   * RMM 1: coleccion identificada mediante URI (JSON sin controles hipermedia).
   * La utilizacion de GET es necesaria en Spring MVC, pero no demuestra
   * por si sola el uso integral de metodos y codigos HTTP del nivel 2.
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public List<PedidoResponse> listarPedidosPorCliente(@RequestParam String cliente) {
    return reporteService.buscarPorCliente(cliente)
        .stream()
        .map(PedidoResponse::from)
        .toList();
  }

  /**
   * RMM 2: recurso de resumen con representacion JSON, GET seguro y
   * estado HTTP explicito 200 OK, sin controles hipermedia.
   */
  @GetMapping(value = "/estadisticas/resumen", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResumenPedidosDto> obtenerResumen() {
    return ResponseEntity.ok(ResumenPedidosDto.from(reporteService.obtenerResumen()));
  }

  /**
   * RMM 3: coleccion de estadisticas con enlaces de navegacion HAL.
   * En un API de produccion conviene aplicar hipermedia de forma consistente.
   */
  @GetMapping(value = "/estadisticas/productos", produces = MediaTypes.HAL_JSON_VALUE)
  public ResponseEntity<CollectionModel<ProductoVendidoDto>> listarProductosMasVendidos() {
    List<ProductoVendidoDto> productos = reporteService.listarProductosMasVendidos()
        .stream().map(ProductoVendidoDto::from).toList();
    CollectionModel<ProductoVendidoDto> coleccion = CollectionModel.of(
        productos,
        linkTo(PedidoReporteController.class).slash("estadisticas/productos").withSelfRel(),
        linkTo(PedidoReporteController.class).slash("estadisticas/resumen")
            .withRel("estadisticas-resumen"),
        linkPedidos().withRel("pedidos")
    );
    return ResponseEntity.ok(coleccion);
  }

  // =====================================================================
  // [PA3] Nuevos endpoints JSON. Delegan en PedidoReporteService:
  // no contienen SQL ni reglas de validación. Los parámetros se declaran
  // opcionales para que la regla "obligatorio" la aplique el Service y el
  // error se traduzca a 400 en ReporteApiExceptionHandler.
  // =====================================================================

  /**
   * [PA3] GET /api/v1/pedidos/busqueda-avanzada?cliente={valor}&amp;totalMinimo={monto}
   *
   * @return 200 con la lista de pedidos (puede estar vacía); 400 si algún
   *         parámetro es inválido
   */
  @GetMapping(value = "/busqueda-avanzada", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<PedidoResponse>> buscarPedidosPorClienteYTotalMinimo(
      @RequestParam(required = false) String cliente,
      @RequestParam(required = false) BigDecimal totalMinimo) {

    List<PedidoResponse> pedidos = reporteService
        .buscarPedidosPorClienteYTotalMinimo(cliente, totalMinimo)
        .stream()
        .map(PedidoResponse::from)
        .toList();

    return ResponseEntity.ok(pedidos);
  }

  /**
   * [PA3] GET /api/v1/pedidos/clientes-frecuentes?minPedidos={n}
   *
   * @return 200 con los clientes frecuentes y sus consolidados; 400 si
   *         minPedidos es inválido
   */
  @GetMapping(value = "/clientes-frecuentes", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<ClienteFrecuenteDto>> listarClientesFrecuentes(
      @RequestParam(required = false) Integer minPedidos) {

    List<ClienteFrecuenteDto> clientes = reporteService
        .listarClientesFrecuentes(minPedidos)
        .stream()
        .map(ClienteFrecuenteDto::from)
        .toList();

    return ResponseEntity.ok(clientes);
  }

  private Link linkPedidos() {
    return Link.of(linkTo(PedidoReporteController.class).toUri() + "{?cliente}");
  }
}

