package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import com.isil.edu.pe.spedidos.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.spedidos.repository.projection.ResumenPedidosProjection;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import com.isil.edu.pe.spedidos.service.domain.ProductoVendido;
import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de los reportes del Sistema de Pedidos.
 *
 * <p>[PA3] Concentra las validaciones de los parámetros de la búsqueda
 * avanzada y de clientes frecuentes. El repositorio JDBC solo recibe valores
 * ya validados; los controladores (JSP y REST) solo traducen el resultado o la
 * excepción a su representación.</p>
 *
 * <p>Recibe {@link PedidoReporteJdbcRepository} por constructor (sin
 * {@code @Autowired}), lo que permite probarla con Mockito sin iniciar Spring.</p>
 */
@Service
@Transactional(readOnly = true)
public class PedidoReporteServiceImpl implements PedidoReporteService {

  /** [PA3] Longitud máxima de la columna pedido.cliente. */
  static final int CLIENTE_MAX_LENGTH = 120;

  private final PedidoReporteJdbcRepository jdbcRepository;

  public PedidoReporteServiceImpl(PedidoReporteJdbcRepository jdbcRepository) {
      this.jdbcRepository = jdbcRepository;
  }

  @Override
  public ResumenPedidos obtenerResumen() {
    ResumenPedidosProjection resumenPedidos = jdbcRepository.obtenerResumen();
    return new ResumenPedidos(resumenPedidos.totalPedidos(), resumenPedidos.totalUnidades(),
        resumenPedidos.importeAcumulado(), resumenPedidos.ticketPromedio());
  }

  @Override
  public List<Pedido> buscarPorCliente(String cliente) {
    if (cliente == null || cliente.isBlank()) {
      return List.of();
    }

    return jdbcRepository.buscarPorCliente(cliente);
  }

  @Override
  public List<ProductoVendido> listarProductosMasVendidos() {
    return jdbcRepository.listarProductosMasVendidos()
        .stream()
        .map(item ->
            new ProductoVendido(
                item.productoCodigo(),
                item.productoNombre(),
                item.totalPedidos(),
                item.totalUnidades(),
                item.importeAcumulado()
            )
        )
        .toList();

  }

  // =====================================================================
  // [PA3] Nuevas operaciones
  // =====================================================================

  @Override
  public List<Pedido> buscarPedidosPorClienteYTotalMinimo(String cliente,
      BigDecimal totalMinimo) {

    validarCliente(cliente);
    validarTotalMinimo(totalMinimo);

    return jdbcRepository.buscarPedidosPorClienteYTotalMinimo(cliente.trim(), totalMinimo);
  }

  @Override
  public List<ClienteFrecuente> listarClientesFrecuentes(Integer minPedidos) {
    validarMinPedidos(minPedidos);

    return jdbcRepository.listarClientesFrecuentes(minPedidos)
        .stream()
        .map(item ->
            new ClienteFrecuente(
                item.cliente(),
                item.totalPedidos(),
                item.totalUnidades(),
                item.importeAcumulado(),
                item.ticketPromedio()
            )
        )
        .toList();
  }

  // =====================================================================
  // [PA3] Reglas de validación (solo en la capa Service)
  // =====================================================================

  private void validarCliente(String cliente) {
    if (cliente == null || cliente.isBlank()) {
      throw new ParametroReporteInvalidoException(
          "cliente", "El parámetro cliente es obligatorio y no puede estar vacío.");
    }
    if (cliente.trim().length() > CLIENTE_MAX_LENGTH) {
      throw new ParametroReporteInvalidoException(
          "cliente", "El parámetro cliente no puede superar "
              + CLIENTE_MAX_LENGTH + " caracteres.");
    }
  }

  private void validarTotalMinimo(BigDecimal totalMinimo) {
    if (totalMinimo == null) {
      throw new ParametroReporteInvalidoException(
          "totalMinimo", "El parámetro totalMinimo es obligatorio.");
    }
    if (totalMinimo.signum() < 0) {
      throw new ParametroReporteInvalidoException(
          "totalMinimo", "El parámetro totalMinimo debe ser mayor o igual a 0.");
    }
  }

  private void validarMinPedidos(Integer minPedidos) {
    if (minPedidos == null) {
      throw new ParametroReporteInvalidoException(
          "minPedidos", "El parámetro minPedidos es obligatorio.");
    }
    if (minPedidos < 1) {
      throw new ParametroReporteInvalidoException(
          "minPedidos", "El parámetro minPedidos debe ser mayor o igual a 1.");
    }
  }
}
