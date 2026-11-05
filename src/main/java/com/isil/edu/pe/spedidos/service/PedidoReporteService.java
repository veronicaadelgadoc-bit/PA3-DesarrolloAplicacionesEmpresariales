package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import com.isil.edu.pe.spedidos.service.domain.ProductoVendido;
import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;
import java.math.BigDecimal;
import java.util.List;

/**
 * Contrato de la capa de servicio para los reportes de solo lectura.
 *
 * <p>La vista JSP ({@code PedidoReporteWebController}) y la API JSON
 * ({@code PedidoReporteController}) utilizan este mismo contrato, por lo que
 * las reglas de validación existen en un solo lugar.</p>
 */
public interface PedidoReporteService {

  ResumenPedidos obtenerResumen();

  List<Pedido> buscarPorCliente(String cliente);

  List<ProductoVendido> listarProductosMasVendidos();

  /**
   * [PA3] Búsqueda avanzada por cliente y monto mínimo.
   *
   * @param cliente     texto del cliente; obligatorio y no vacío
   * @param totalMinimo importe mínimo; obligatorio y mayor o igual a 0
   * @return pedidos que cumplen ambos filtros, ordenados por id descendente
   * @throws ParametroReporteInvalidoException si algún parámetro es inválido
   */
  List<Pedido> buscarPedidosPorClienteYTotalMinimo(String cliente, BigDecimal totalMinimo);

  /**
   * [PA3] Clientes con al menos {@code minPedidos} pedidos y sus consolidados.
   *
   * @param minPedidos umbral de pedidos; obligatorio y mayor o igual a 1
   * @return clientes frecuentes con totalPedidos, totalUnidades,
   *         importeAcumulado y ticketPromedio
   * @throws ParametroReporteInvalidoException si el umbral es inválido
   */
  List<ClienteFrecuente> listarClientesFrecuentes(Integer minPedidos);

}
