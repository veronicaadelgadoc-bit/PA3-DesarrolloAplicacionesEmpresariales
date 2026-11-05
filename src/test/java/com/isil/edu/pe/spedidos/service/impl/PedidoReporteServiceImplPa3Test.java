package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import com.isil.edu.pe.spedidos.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.spedidos.repository.projection.ClienteFrecuenteProjection;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * [PA3] Pruebas unitarias de las validaciones de {@link PedidoReporteServiceImpl}.
 *
 * <p>El repositorio JDBC se reemplaza por un mock de Mockito; gracias a la
 * inyección por constructor, el servicio se crea con {@code new} sin iniciar
 * Spring. Las pruebas de rechazo verifican además que el repositorio
 * <strong>no</strong> fue invocado: un parámetro inválido nunca llega a SQL.</p>
 */
class PedidoReporteServiceImplPa3Test {

  private PedidoReporteJdbcRepository jdbcRepository;

  private PedidoReporteServiceImpl service;

  @BeforeEach
  void setUp() {
    jdbcRepository = mock(PedidoReporteJdbcRepository.class);
    service = new PedidoReporteServiceImpl(jdbcRepository);
  }

  @Test
  @DisplayName("Reject listarClientesFrecuentes without querying repository when minPedidos is zero")
  void rejectListarClientesFrecuentesWithoutQueryingRepositoryWhenMinPedidosIsZero() {
    // Arrange
    Integer minPedidos = 0;

    // Act
    ParametroReporteInvalidoException ex = assertThrows(
        ParametroReporteInvalidoException.class,
        () -> service.listarClientesFrecuentes(minPedidos));

    // Assert
    assertEquals("minPedidos", ex.getParametro());
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Reject buscarPedidosPorClienteYTotalMinimo without querying repository when totalMinimo is negative")
  void rejectAdvancedSearchWithoutQueryingRepositoryWhenTotalMinimoIsNegative() {
    // Arrange
    BigDecimal totalMinimo = new BigDecimal("-1");

    // Act
    ParametroReporteInvalidoException ex = assertThrows(
        ParametroReporteInvalidoException.class,
        () -> service.buscarPedidosPorClienteYTotalMinimo("Ana", totalMinimo));

    // Assert
    assertEquals("totalMinimo", ex.getParametro());
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Reject buscarPedidosPorClienteYTotalMinimo without querying repository when cliente is blank")
  void rejectAdvancedSearchWithoutQueryingRepositoryWhenClienteIsBlank() {
    // Arrange
    String cliente = "   ";

    // Act
    ParametroReporteInvalidoException ex = assertThrows(
        ParametroReporteInvalidoException.class,
        () -> service.buscarPedidosPorClienteYTotalMinimo(cliente, BigDecimal.TEN));

    // Assert
    assertEquals("cliente", ex.getParametro());
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Reject buscarPedidosPorClienteYTotalMinimo when totalMinimo is missing")
  void rejectAdvancedSearchWhenTotalMinimoIsMissing() {
    // Arrange / Act
    ParametroReporteInvalidoException ex = assertThrows(
        ParametroReporteInvalidoException.class,
        () -> service.buscarPedidosPorClienteYTotalMinimo("Ana", null));

    // Assert
    assertEquals("totalMinimo", ex.getParametro());
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Delegate trimmed cliente and totalMinimo to repository when parameters are valid")
  void delegateTrimmedParametersToRepositoryWhenAdvancedSearchParametersAreValid() {
    // Arrange
    BigDecimal totalMinimo = new BigDecimal("1000");
    List<Pedido> esperado = List.of(new Pedido());
    when(jdbcRepository.buscarPedidosPorClienteYTotalMinimo("Ana", totalMinimo))
        .thenReturn(esperado);

    // Act
    List<Pedido> resultado = service.buscarPedidosPorClienteYTotalMinimo("  Ana  ", totalMinimo);

    // Assert
    assertSame(esperado, resultado);
    verify(jdbcRepository).buscarPedidosPorClienteYTotalMinimo("Ana", totalMinimo);
  }

  @Test
  @DisplayName("Map repository projection to domain model when minPedidos is valid")
  void mapProjectionToDomainModelWhenMinPedidosIsValid() {
    // Arrange
    ClienteFrecuenteProjection fila = new ClienteFrecuenteProjection(
        "Ana Torres", 3, 4, new BigDecimal("4320.00"), new BigDecimal("1440.00"));
    when(jdbcRepository.listarClientesFrecuentes(2)).thenReturn(List.of(fila));

    // Act
    List<ClienteFrecuente> clientes = service.listarClientesFrecuentes(2);

    // Assert
    assertEquals(1, clientes.size());
    ClienteFrecuente ana = clientes.get(0);
    assertEquals("Ana Torres", ana.cliente());
    assertEquals(3L, ana.totalPedidos());
    assertEquals(4L, ana.totalUnidades());
    assertEquals(new BigDecimal("4320.00"), ana.importeAcumulado());
    assertEquals(new BigDecimal("1440.00"), ana.ticketPromedio());
    verify(jdbcRepository).listarClientesFrecuentes(2);
  }
}
